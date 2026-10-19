"""Self-tests for plan_tool.py. Each test works on a throwaway copy of the plan and specification."""
import json, shutil, subprocess, sys, tempfile, unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
IMPL_REL = Path("impl/core/amk-authority")
SPEC_REL = Path("specs/core/amk-authority")


def make_tree():
    tmp = Path(tempfile.mkdtemp(prefix="amk-plan-test-"))
    ignore = shutil.ignore_patterns("__pycache__", "agent-reports", "node_modules")
    shutil.copytree(REPO / IMPL_REL, tmp / IMPL_REL, ignore=ignore)
    shutil.copytree(REPO / SPEC_REL, tmp / SPEC_REL, ignore=ignore)
    # The tests describe a plan with nothing done, so they must not change as tasks are recorded done.
    reset = False
    for f in sorted((tmp / IMPL_REL / "plan" / "tasks").glob("*.json")):
        t = json.loads(f.read_text())
        if t["status"] == "done":
            t["status"], t["evidence"] = "todo", {}
            f.write_text(json.dumps(t, indent=2) + "\n")
            reset = True
    if reset:
        subprocess.run([sys.executable, str(tmp / IMPL_REL / "plan" / "plan_tool.py"), "render"],
                       cwd=tmp, check=True, capture_output=True)
    return tmp


class PlanToolTests(unittest.TestCase):
    def setUp(self):
        self.tmp = make_tree()
        self.plan = self.tmp / IMPL_REL / "plan"
        self.tasks = self.plan / "tasks"
        self.addCleanup(shutil.rmtree, self.tmp, True)

    def run_tool(self, *args):
        r = subprocess.run([sys.executable, str(self.plan / "plan_tool.py"), *args],
                           capture_output=True, text=True, cwd=self.tmp)
        return r.returncode, r.stdout + r.stderr

    def live_task(self):
        for f in sorted(self.tasks.glob("*.json")):
            t = json.loads(f.read_text())
            if t["status"] == "todo" and not t["deliverables"]:
                return f, t
        self.fail("no suitable task found")

    def write(self, f, t):
        f.write_text(json.dumps(t, indent=2) + "\n")

    def test_baseline_passes(self):
        code, out = self.run_tool("check")
        self.assertEqual(code, 0, out)

    def test_duplicate_id_in_earlier_sorted_file_is_rejected(self):
        src = json.loads((self.tasks / "AUT-012.json").read_text())
        src["title"] = "SHADOWED COPY"
        self.write(self.tasks / "AUT-000.json", src)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("duplicate task id AUT-012", out)
        self.assertIn("AUT-000.json", out)
        self.assertIn("AUT-012.json", out)

    def test_wrong_types_are_errors_not_tracebacks(self):
        f, t = self.live_task()
        t["verify"] = "manual"
        t["acceptance"] = "must work"
        t["depends_on"] = "AUT-012"
        t["bogus_field"] = 1
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertNotIn("Traceback", out)
        for needle in ("'verify' must be an object", "'acceptance' must be a list of strings",
                       "'depends_on' must be a list of strings", "unknown field 'bogus_field'"):
            self.assertIn(needle, out)

    def test_spec_ref_in_prose_only_is_rejected(self):
        f, t = self.live_task()
        t["spec_refs"] = ["E-ATA-12"]
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("E-ATA-12", out)
        self.assertIn("not defined", out)

    def test_spec_ref_to_summary_or_reserved_is_rejected(self):
        f, t = self.live_task()
        t["spec_refs"] = ["ATA-06:08", "ATA-01:06"]
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("ATA-06:08", out)
        self.assertIn("ATA-01:06", out)
        self.assertIn("informative", out)
        self.assertIn("reserved", out)

    def test_done_with_fake_evidence_is_rejected(self):
        f, t = self.live_task()
        t["status"] = "done"
        t["evidence"] = {"pr": "x", "commit": "not-a-sha", "verified_at": "2099-12-31"}
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        for needle in ("evidence.pr", "evidence.commit", "in the future", "verified_by", "evidence.verification"):
            self.assertIn(needle, out)

    def test_done_with_complete_evidence_passes(self):
        f, t = self.live_task()
        for dep in t["depends_on"]:
            d = self.tasks / f"{dep}.json"
            dt = json.loads(d.read_text())
            dt["status"] = "dropped"
        t["status"] = "done"
        t["depends_on"] = []
        t["verify"] = {"method": "manual"}
        t["evidence"] = {"pr": "https://github.com/inscope-labs/osx/pull/1", "commit": "abc1234",
                         "verified_at": "2026-10-17", "verified_by": "Agent A",
                         "verification": {"method": "manual", "result": "pass"}, "notes": "each statement mapped"}
        self.write(f, t)
        # stage gates must not block this task
        stages = json.loads((self.plan / "stages.json").read_text())
        for s in stages["stages"]:
            s["gate"] = False
        (self.plan / "stages.json").write_text(json.dumps(stages))
        code, out = self.run_tool("check")
        self.assertEqual(code, 0, out)

    def test_live_task_depending_on_dropped_task_is_rejected(self):
        dropped = next(json.loads(p.read_text()) for p in sorted(self.tasks.glob("*.json"))
                       if json.loads(p.read_text())["status"] == "dropped")
        f, t = self.live_task()
        t["depends_on"] = [dropped["id"]]
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("depends on dropped task", out)

    def test_dropped_task_requires_superseded_by(self):
        f, t = self.live_task()
        t["status"] = "dropped"
        t["dropped_reason"] = "test"
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("superseded_by", out)

    def test_coverage_reports_uncited_items_and_fails(self):
        for p in self.tasks.glob("*.json"):
            t = json.loads(p.read_text())
            t["spec_refs"] = [r for r in t["spec_refs"] if r != "INV-ATA-07"]
            self.write(p, t)
        code, out = self.run_tool("coverage", "--fail")
        self.assertNotEqual(code, 0)
        self.assertIn("UNCOVERED INV-ATA-07", out)
        code2, _ = self.run_tool("coverage")
        self.assertEqual(code2, 0)

    def test_render_is_current_and_detects_staleness(self):
        code, out = self.run_tool("render", "--check")
        self.assertEqual(code, 0, out)
        p = self.tmp / IMPL_REL / "PLAN.md"
        p.write_text(p.read_text() + "\nstale\n")
        code, out = self.run_tool("render", "--check")
        self.assertNotEqual(code, 0)

    def test_next_lists_ready_and_stage_eligible(self):
        code, out = self.run_tool("next", "--json")
        self.assertEqual(code, 0, out)
        data = json.loads(out)
        self.assertEqual([r["id"] for r in data["ready"]], ["AUT-007", "AUT-061"])
        s0 = data["stage_eligible"]["S0"]
        self.assertEqual(s0["tasks"][0], "AUT-007")
        self.assertIn("AUT-011", s0["operator_remaining"])
        self.assertFalse(s0["completes_stage"])

    def test_next_assume_done_unlocks_dependents(self):
        code, out = self.run_tool("next", "--assume-done", "AUT-007", "--json")
        self.assertEqual(code, 0, out)
        self.assertIn("AUT-008", [r["id"] for r in json.loads(out)["ready"]])

    def test_order_rejects_unmet_dependencies_and_gates(self):
        code, out = self.run_tool("order", "AUT-013")
        self.assertNotEqual(code, 0)
        self.assertIn("neither done nor earlier", out)
        self.assertIn("gate stage", out)

    def test_order_rejects_operator_tasks(self):
        code, out = self.run_tool("order", "AUT-061")
        self.assertNotEqual(code, 0)
        self.assertIn("cannot be given to a worker", out)

    def test_order_individual_and_group_are_deterministic(self):
        code, out = self.run_tool("order", "AUT-007", "--json")
        self.assertEqual(code, 0, out)
        one = json.loads(out)
        self.assertEqual(one["mode"], "individual")
        self.assertTrue(one["submission_id"].startswith("AUT-007-"))
        self.assertTrue(one["upload_to"].endswith(one["submission_id"] + "/"))
        self.assertEqual(list(one["task_dirs"]), ["AUT-007"])
        self.assertTrue(one["task_dirs"]["AUT-007"].startswith("AUT-007-"))
        c1, o1 = self.run_tool("order", "AUT-008", "AUT-007", "--json")
        c2, o2 = self.run_tool("order", "AUT-007", "AUT-008", "--json")
        self.assertEqual((c1, o1), (c2, o2))
        grp = json.loads(o1)
        self.assertEqual(grp["tasks"], ["AUT-007", "AUT-008"])
        self.assertEqual(grp["mode"], "group")
        self.assertIn("impl/core/amk-authority/plan/tasks/AUT-008.json", grp["package"])

    def test_executor_value_is_validated(self):
        f, t = self.live_task()
        t["executor"] = "robot"
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("'executor' must be one of", out)

    def test_worker_task_without_deliverables_is_rejected(self):
        f, t = self.live_task()
        t["deliverables"] = []
        t["verify"] = {"method": "manual"}
        t.pop("executor", None)
        self.write(f, t)
        code, out = self.run_tool("check")
        self.assertNotEqual(code, 0)
        self.assertIn("needs at least one deliverable", out)


if __name__ == "__main__":
    unittest.main()
