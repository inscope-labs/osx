#!/usr/bin/env python3
"""amk-authority implementation plan tool (standard library only).

Source of truth:
  plan/stages.json        stage boundaries (title, goal, exit criteria, gate flag)
  plan/tasks/AUT-nnn.json one file per atomic task (status + verification live here)
Generated:
  PLAN.md                 never edit by hand; run `render`

Commands:
  check                   validate schema, dependencies, gates, evidence, spec references
  status                  print per-stage and overall task counts
  render [--check]        write PLAN.md (or fail if it is out of date)
  verify ID               run the verification for one task (files / command); does not change status
  set ID STATUS [opts]    change a task's status with evidence, then re-run check
"""
import argparse, datetime, json, re, subprocess, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
IMPL = HERE.parent
ROOT = IMPL.parents[2]
SPEC_DIR = ROOT / "specs" / "core" / "amk-authority"
TASK_DIR = HERE / "tasks"
STAGES_FILE = HERE / "stages.json"
PLAN_MD = IMPL / "PLAN.md"

STATUSES = ["todo", "in_progress", "blocked", "done", "dropped"]
ID_RE = re.compile(r"^AUT-\d{3}$")
VERIFY_METHODS = {"manual", "files", "command"}
REQUIRED = ["id", "stage", "title", "description", "spec_refs", "depends_on",
            "deliverables", "acceptance", "verify", "status", "blocked_by",
            "evidence", "dropped_reason"]


def load_stages():
    return json.loads(STAGES_FILE.read_text())["stages"]


def load_tasks():
    tasks, problems = {}, []
    for f in sorted(TASK_DIR.glob("*.json")):
        try:
            t = json.loads(f.read_text())
        except json.JSONDecodeError as e:
            problems.append(f"{f.name}: invalid JSON ({e})")
            continue
        t["_file"] = f
        tasks[t.get("id", f.stem)] = t
    return tasks, problems


def spec_text():
    return "\n".join(p.read_text() for p in sorted(SPEC_DIR.glob("*.md"))
                     if not p.name.endswith("-spec-full.md"))


def satisfied(tasks, tid):
    return tasks[tid]["status"] in ("done", "dropped")


def stage_state(tasks, sid):
    ts = [t for t in tasks.values() if t["stage"] == sid and t["status"] != "dropped"]
    if not ts:
        return "empty"
    if all(t["status"] == "done" for t in ts):
        return "done"
    if any(t["status"] in ("in_progress", "done") for t in ts):
        return "in_progress"
    if all(t["status"] == "blocked" for t in ts):
        return "blocked"
    return "todo"


def check():
    stages = load_stages()
    sids = [s["id"] for s in stages]
    errs = []
    if len(set(sids)) != len(sids):
        errs.append("stages.json: duplicate stage id")
    tasks, problems = load_tasks()
    errs += problems
    text = spec_text()
    for tid, t in tasks.items():
        p = f"{tid}"
        for k in REQUIRED:
            if k not in t:
                errs.append(f"{p}: missing field '{k}'")
        if any(k not in t for k in REQUIRED):
            continue
        if not ID_RE.match(tid) or t["_file"].stem != tid:
            errs.append(f"{p}: id must match AUT-nnn and the file name ({t['_file'].name})")
        if t["stage"] not in sids:
            errs.append(f"{p}: unknown stage '{t['stage']}'")
        if t["status"] not in STATUSES:
            errs.append(f"{p}: invalid status '{t['status']}'")
        if not t["acceptance"]:
            errs.append(f"{p}: acceptance must list at least one checkable statement")
        v = t["verify"]
        if v.get("method") not in VERIFY_METHODS:
            errs.append(f"{p}: verify.method must be one of {sorted(VERIFY_METHODS)}")
        if v.get("method") == "command" and not v.get("command"):
            errs.append(f"{p}: verify.command is required for method 'command'")
        if v.get("method") == "files" and not t["deliverables"]:
            errs.append(f"{p}: method 'files' requires deliverables")
        for ref in t["spec_refs"]:
            if ref not in text:
                errs.append(f"{p}: spec_ref '{ref}' not found in specs/core/amk-authority")
        for d in t["depends_on"]:
            if d == tid:
                errs.append(f"{p}: depends on itself")
            elif d not in tasks:
                errs.append(f"{p}: depends_on unknown task '{d}'")
        st = t["status"]
        if st == "blocked" and not t["blocked_by"]:
            errs.append(f"{p}: blocked requires blocked_by")
        if st == "dropped" and not t["dropped_reason"].strip():
            errs.append(f"{p}: dropped requires dropped_reason")
        if st == "done":
            ev = t["evidence"]
            if not (ev.get("pr") or ev.get("commit")):
                errs.append(f"{p}: done requires evidence.pr or evidence.commit")
            if not ev.get("verified_at"):
                errs.append(f"{p}: done requires evidence.verified_at")
            for d in t["deliverables"]:
                if not (ROOT / d).exists():
                    errs.append(f"{p}: done but deliverable missing: {d}")
    # dependency integrity and cycles
    for tid, t in tasks.items():
        if "depends_on" not in t:
            continue
        for d in t["depends_on"]:
            if d in tasks and "status" in t and t["status"] in ("in_progress", "done") and not satisfied(tasks, d):
                errs.append(f"{tid}: {t['status']} but dependency {d} is {tasks[d]['status']}")
    seen, stack = set(), []

    def dfs(n):
        if n in stack:
            errs.append("dependency cycle: " + " -> ".join(stack[stack.index(n):] + [n]))
            return
        if n in seen or n not in tasks:
            return
        stack.append(n)
        for d in tasks[n].get("depends_on", []):
            dfs(d)
        stack.pop()
        seen.add(n)

    for n in tasks:
        dfs(n)
    # stage gates: nothing in a later stage may start while an earlier gate stage is open
    order = {s["id"]: i for i, s in enumerate(stages)}
    for s in stages:
        if not s.get("gate"):
            continue
        if stage_state(tasks, s["id"]) in ("done", "empty"):
            continue
        for tid, t in tasks.items():
            if t.get("stage") in order and order[t["stage"]] > order[s["id"]] \
                    and t.get("status") in ("in_progress", "done"):
                errs.append(f"{tid}: {t['status']} in stage {t['stage']} while gate stage {s['id']} is not complete")
    return stages, tasks, errs


def counts(tasks, sid=None):
    c = {s: 0 for s in STATUSES}
    for t in tasks.values():
        if sid is None or t["stage"] == sid:
            c[t["status"]] += 1
    return c


def cmd_status(_):
    stages, tasks, errs = check()
    for s in stages:
        c = counts(tasks, s["id"])
        total = sum(c.values())
        print(f"{s['id']:<3} {s['title']:<42} {stage_state(tasks, s['id']):<12} "
              f"done {c['done']}/{total - c['dropped']}  in_progress {c['in_progress']}  blocked {c['blocked']}  dropped {c['dropped']}")
    c = counts(tasks)
    print(f"ALL tasks: {sum(c.values())}  " + "  ".join(f"{k} {v}" for k, v in c.items()))
    if errs:
        print(f"\n{len(errs)} problem(s); run `check`")
    return 1 if errs else 0


def cmd_check(_):
    stages, tasks, errs = check()
    for e in errs:
        print("ERROR:", e)
    print(f"{len(tasks)} tasks, {len(stages)} stages, {len(errs)} error(s)")
    return 1 if errs else 0


def render():
    stages, tasks, errs = check()
    c = counts(tasks)
    out = ["# amk-authority implementation plan", "",
           "> **Generated file. Do not edit.** Edit `plan/stages.json` or `plan/tasks/AUT-nnn.json`, then run `python3 impl/core/amk-authority/plan/plan_tool.py render`.", "",
           "Stages are boundaries only. All work and all verification live in atomic tasks. A stage's state is derived from its tasks and is never stored.", "",
           f"**Tasks:** {sum(c.values())} total · " + " · ".join(f"{k} {v}" for k, v in c.items()), "",
           "## How to change this plan", "",
           "- **Add a task:** create `plan/tasks/AUT-nnn.json` with the next free number (IDs are never reused or reassigned) and fill every field.",
           "- **Adjust a task:** edit its file. Moving it to another stage is a one-field change (`stage`).",
           "- **Remove a task:** delete the file only if it was never started. Otherwise set `status` to `dropped` with a `dropped_reason`, so history stays traceable.",
           "- **Add, reorder or remove a stage:** edit `plan/stages.json`. A stage can only be removed when no task refers to it.",
           "- **Complete a task:** all acceptance statements are met, its verification passes, and `status` is `done` with `evidence.pr` or `evidence.commit` and `evidence.verified_at`. The `set` command writes this for you.",
           "- Run `check` before every plan PR. A task cannot be `in_progress` or `done` while a dependency is open, or while an earlier gate stage is incomplete.", "",
           "## Statuses", "",
           "| Status | Meaning | Required |",
           "|---|---|---|",
           "| `todo` | Not started | none |",
           "| `in_progress` | Being worked on | dependencies done or dropped; earlier gate stages complete |",
           "| `blocked` | Cannot proceed | `blocked_by` (task IDs, open items, or a decision) |",
           "| `done` | Acceptance met and verified | `evidence.pr` or `evidence.commit`, `evidence.verified_at`, deliverables present |",
           "| `dropped` | No longer needed | `dropped_reason` |", "",
           "Verification methods: `manual` (reviewer confirms every acceptance statement in the PR), `files` (deliverables exist), `command` (`verify ID` runs `verify.command` from `impl/core/amk-authority`).", ""]
    for s in stages:
        cs = counts(tasks, s["id"])
        total = sum(cs.values()) - cs["dropped"]
        out += [f"## {s['id']}: {s['title']}" + ("  (gate)" if s.get("gate") else ""), "",
                f"**State:** {stage_state(tasks, s['id'])} ({cs['done']}/{total} done)", "",
                f"**Goal:** {s['goal']}", "", f"**Exit criteria:** {s['exit']}", ""]
        rows = [t for t in tasks.values() if t["stage"] == s["id"]]
        if rows:
            out += ["| Task | Title | Status | Depends on | Verify | Spec refs |", "|---|---|---|---|---|---|"]
            for t in sorted(rows, key=lambda x: x["id"]):
                out.append(f"| `{t['id']}` | {t['title']} | {t['status']} | "
                           f"{', '.join(t['depends_on']) or '-'} | {t['verify']['method']} | "
                           f"{', '.join(t['spec_refs']) or '-'} |")
            out.append("")
    out += ["---", "",
            "`E-ATA-nn` identifiers in task `findings` refer to the cursory review findings in the amk-authority specification and are informational only."]
    return "\n".join(out) + "\n", errs


def cmd_render(a):
    text, errs = render()
    if a.check:
        if not PLAN_MD.exists() or PLAN_MD.read_text() != text:
            print("ERROR: PLAN.md is out of date; run `render`")
            return 1
        print("PLAN.md is up to date")
        return 0
    PLAN_MD.write_text(text)
    print(f"wrote {PLAN_MD.relative_to(ROOT)}")
    return 1 if errs else 0


def cmd_verify(a):
    _, tasks, _ = check()
    t = tasks.get(a.id)
    if not t:
        print(f"ERROR: unknown task {a.id}")
        return 1
    v, ok = t["verify"], True
    print(f"{a.id}: {t['title']}  [status {t['status']}, method {v['method']}]")
    for d in t["deliverables"]:
        exists = (ROOT / d).exists()
        print(f"  deliverable {'OK     ' if exists else 'MISSING'} {d}")
        ok &= exists
    if v["method"] == "command":
        print(f"  $ {v['command']}")
        r = subprocess.run(v["command"], shell=True, cwd=IMPL)
        ok &= r.returncode == 0
    if v["method"] == "manual":
        print("  manual: confirm each acceptance statement in the PR review:")
    for x in t["acceptance"]:
        print(f"   - {x}")
    print("RESULT:", "PASS (automatic checks)" if ok else "FAIL")
    return 0 if ok else 1


def cmd_set(a):
    f = TASK_DIR / f"{a.id}.json"
    if not f.exists():
        print(f"ERROR: unknown task {a.id}")
        return 1
    t = json.loads(f.read_text())
    t["status"] = a.status
    if a.status == "done":
        t["evidence"] = {"pr": a.pr or "", "commit": a.commit or "",
                         "verified_at": datetime.date.today().isoformat(),
                         "verified_by": a.by or "", "notes": a.notes or ""}
    if a.status == "blocked":
        t["blocked_by"] = a.blocked_by or t["blocked_by"]
    if a.status == "dropped":
        t["dropped_reason"] = a.reason or ""
    f.write_text(json.dumps(t, indent=2) + "\n")
    return cmd_check(a)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("check").set_defaults(fn=cmd_check)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    r = sub.add_parser("render"); r.add_argument("--check", action="store_true"); r.set_defaults(fn=cmd_render)
    v = sub.add_parser("verify"); v.add_argument("id"); v.set_defaults(fn=cmd_verify)
    s = sub.add_parser("set"); s.add_argument("id"); s.add_argument("status", choices=STATUSES)
    s.add_argument("--pr"); s.add_argument("--commit"); s.add_argument("--by"); s.add_argument("--notes")
    s.add_argument("--reason"); s.add_argument("--blocked-by", nargs="*", dest="blocked_by")
    s.set_defaults(fn=cmd_set)
    a = p.parse_args()
    sys.exit(a.fn(a))


if __name__ == "__main__":
    main()
