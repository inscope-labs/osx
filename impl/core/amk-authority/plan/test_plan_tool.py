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


if __name__ == "__main__":
    unittest.main()
