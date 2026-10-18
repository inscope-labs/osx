#!/usr/bin/env python3
"""amk-authority implementation plan tool (standard library only).

Source of truth:
  plan/stages.json          stage boundaries (title, goal, exit criteria, gate flag)
  plan/tasks/AUT-nnn.json   one file per atomic task
  plan/coverage_exempt.json specification items intentionally not cited by a task, with reasons
Generated:
  PLAN.md                   never edit by hand; run `render`

Commands:
  check                     validate schema, IDs, dependencies, gates, evidence and specification references
  status                    per-stage and overall task counts
  render [--check]          write PLAN.md (or fail if it is out of date)
  verify ID                 run the verification for one task; does not change status
  set ID STATUS [options]   change a task's status; `done` runs the verification and records evidence
  registry                  list every specification item a task may cite
  coverage [--fail]         list normative specification items that no live task cites
"""
import argparse, datetime, json, re, subprocess, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
IMPL = HERE.parent
ROOT = IMPL.parents[2]
SPEC_DIR = ROOT / "specs" / "core" / "amk-authority"
TASK_DIR = HERE / "tasks"
STAGES_FILE = HERE / "stages.json"
EXEMPT_FILE = HERE / "coverage_exempt.json"
PLAN_MD = IMPL / "PLAN.md"

STATUSES = ["todo", "in_progress", "blocked", "done", "dropped"]
ID_RE = re.compile(r"^AUT-\d{3}$")
PR_RE = re.compile(r"^https://github\.com/inscope-labs/osx/pull/\d+$")
SHA_RE = re.compile(r"^[0-9a-f]{7,40}$")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
VERIFY_METHODS = {"manual", "files", "command"}
REQUIRED = ["id", "stage", "title", "description", "spec_refs", "depends_on",
            "deliverables", "acceptance", "verify", "status", "blocked_by",
            "evidence", "dropped_reason"]
OPTIONAL = ["findings", "superseded_by"]
STR_FIELDS = ["id", "stage", "title", "description", "dropped_reason"]
LIST_FIELDS = ["spec_refs", "depends_on", "deliverables", "acceptance", "blocked_by"]
EVIDENCE_KEYS = {"pr", "commit", "verified_at", "verified_by", "verification", "notes"}


def is_str_list(v):
    return isinstance(v, list) and all(isinstance(x, str) for x in v)


def load_stages():
    return json.loads(STAGES_FILE.read_text())["stages"]


# ----------------------------------------------------------------- tasks
def validate_schema(t, fname):
    """Return a list of schema errors for one task object (no exceptions)."""
    p, errs = fname, []
    if not isinstance(t, dict):
        return [f"{p}: task file must contain a JSON object"]
    for k in REQUIRED:
        if k not in t:
            errs.append(f"{p}: missing field '{k}'")
    for k in t:
        if k not in REQUIRED and k not in OPTIONAL:
            errs.append(f"{p}: unknown field '{k}'")
    for k in STR_FIELDS:
        if k in t and not isinstance(t[k], str):
            errs.append(f"{p}: '{k}' must be a string")
    if "findings" in t and not isinstance(t["findings"], str):
        errs.append(f"{p}: 'findings' must be a string")
    for k in LIST_FIELDS:
        if k in t and not is_str_list(t[k]):
            errs.append(f"{p}: '{k}' must be a list of strings")
    if "superseded_by" in t and not is_str_list(t["superseded_by"]):
        errs.append(f"{p}: 'superseded_by' must be a list of strings")
    if isinstance(t.get("title"), str) and not t["title"].strip():
        errs.append(f"{p}: 'title' must not be empty")
    if is_str_list(t.get("acceptance")):
        if not t["acceptance"]:
            errs.append(f"{p}: acceptance must list at least one checkable statement")
        if any(not x.strip() for x in t["acceptance"]):
            errs.append(f"{p}: acceptance statements must not be empty")
    v = t.get("verify")
    if "verify" in t:
        if not isinstance(v, dict):
            errs.append(f"{p}: 'verify' must be an object with a 'method'")
        else:
            for k in v:
                if k not in ("method", "command"):
                    errs.append(f"{p}: unknown field 'verify.{k}'")
            if v.get("method") not in VERIFY_METHODS:
                errs.append(f"{p}: verify.method must be one of {sorted(VERIFY_METHODS)}")
            if v.get("method") == "command" and not (isinstance(v.get("command"), str) and v["command"].strip()):
                errs.append(f"{p}: verify.command is required for method 'command'")
            if v.get("method") == "files" and is_str_list(t.get("deliverables")) and not t["deliverables"]:
                errs.append(f"{p}: method 'files' requires deliverables")
    if "status" in t and t["status"] not in STATUSES:
        errs.append(f"{p}: invalid status '{t['status']}'")
    ev = t.get("evidence")
    if "evidence" in t and not isinstance(ev, dict):
        errs.append(f"{p}: 'evidence' must be an object")
    elif isinstance(ev, dict):
        for k in ev:
            if k not in EVIDENCE_KEYS:
                errs.append(f"{p}: unknown field 'evidence.{k}'")
    return errs


def load_tasks():
    """Load every task file. Duplicate IDs are rejected and both files are named."""
    tasks, errs, seen = {}, [], {}
    for f in sorted(TASK_DIR.glob("*.json")):
        try:
            t = json.loads(f.read_text())
        except json.JSONDecodeError as e:
            errs.append(f"{f.name}: invalid JSON ({e})")
            continue
        serrs = validate_schema(t, f.name)
        tid = t.get("id") if isinstance(t, dict) else None
        if isinstance(tid, str):
            if tid in seen:
                errs.append(f"duplicate task id {tid} in {seen[tid]} and {f.name}")
                continue
            seen[tid] = f.name
            if not ID_RE.match(tid) or f.stem != tid:
                serrs.append(f"{f.name}: id '{tid}' must match AUT-nnn and the file name")
        errs += serrs
        if serrs or not isinstance(tid, str):
            continue
        t["_file"] = f
        tasks[tid] = t
    return tasks, errs


# ----------------------------------------------------------------- specification registry
def build_registry():
    """Every specification item a task may cite: id -> {kind, file, title, status}.

    status: normative | tracking | informative | reserved | retired
    """
    reg = {}
    files = sorted(p for p in SPEC_DIR.glob("*.md") if not p.name.endswith("-spec-full.md"))
    for f in files:
        lines = f.read_text().split("\n")
        for i, line in enumerate(lines):
            m = re.match(r"^## (ATA-\d\d:\d\d) (.+)$", line)
            if m:
                rid, title = m.group(1), m.group(2).strip()
                body = next((l for l in lines[i + 1:] if l.strip()), "")
                if "RETIRED" in title.upper():
                    status = "retired"
                elif body.startswith("- Reserved"):
                    status = "reserved"
                elif f.name == "99-open-items.md":
                    status = "tracking"
                elif title == "Summary":
                    status = "informative"
                else:
                    status = "normative"
                reg[rid] = dict(kind="section", file=f.name, title=title, status=status)
            m = re.match(r"^- (INV-ATA-\d\d):", line)
            if m and f.name == "00-foundations.md":
                reg[m.group(1)] = dict(kind="invariant", file=f.name, title=line[2:70], status="normative")
            m = re.match(r"^- .*\[(DV-ATA-\d\d)\]\s*$", line)
            if m and f.name == "amk-authority.md":
                reg[m.group(1)] = dict(kind="default", file=f.name, title=line[2:70], status="normative")
            m = re.match(r"^- \*\*(O-ATA-\d\d)\*\*", line)
            if m and f.name == "99-open-items.md":
                reg[m.group(1)] = dict(kind="open-item", file=f.name, title=line[2:70], status="tracking")
    return reg


def load_exempt():
    if not EXEMPT_FILE.exists():
        return {}
    data = json.loads(EXEMPT_FILE.read_text())
    return {e["id"]: e.get("reason", "") for e in data.get("exempt", [])}


# ----------------------------------------------------------------- state and checks
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


def validate_evidence(tid, t):
    errs, ev = [], t["evidence"]
    if not PR_RE.match(str(ev.get("pr", ""))):
        errs.append(f"{tid}: done requires evidence.pr as https://github.com/inscope-labs/osx/pull/<n>")
    if not SHA_RE.match(str(ev.get("commit", ""))):
        errs.append(f"{tid}: done requires evidence.commit as a 7 to 40 character lowercase hex SHA")
    va = str(ev.get("verified_at", ""))
    if not DATE_RE.match(va):
        errs.append(f"{tid}: done requires evidence.verified_at as YYYY-MM-DD")
    else:
        try:
            if datetime.date.fromisoformat(va) > datetime.date.today():
                errs.append(f"{tid}: evidence.verified_at is in the future")
        except ValueError:
            errs.append(f"{tid}: evidence.verified_at is not a valid date")
    if not (isinstance(ev.get("verified_by"), str) and ev["verified_by"].strip()):
        errs.append(f"{tid}: done requires evidence.verified_by")
    vr = ev.get("verification")
    if not isinstance(vr, dict):
        errs.append(f"{tid}: done requires evidence.verification (method and result)")
    else:
        if vr.get("method") != t["verify"].get("method"):
            errs.append(f"{tid}: evidence.verification.method must equal verify.method")
        if vr.get("result") != "pass":
            errs.append(f"{tid}: evidence.verification.result must be 'pass'")
        if t["verify"].get("method") == "command" and vr.get("command") != t["verify"].get("command"):
            errs.append(f"{tid}: evidence.verification.command must equal verify.command")
        if t["verify"].get("method") == "manual" and not str(ev.get("notes", "")).strip():
            errs.append(f"{tid}: manual verification requires evidence.notes mapping each acceptance statement")
    return errs


def check():
    stages = load_stages()
    sids = [s["id"] for s in stages]
    errs = []
    if len(set(sids)) != len(sids):
        errs.append("stages.json: duplicate stage id")
    tasks, lerrs = load_tasks()
    errs += lerrs
    registry = build_registry()
    for tid, t in tasks.items():
        if t["stage"] not in sids:
            errs.append(f"{tid}: unknown stage '{t['stage']}'")
        for ref in t["spec_refs"]:
            r = registry.get(ref)
            if r is None:
                errs.append(f"{tid}: spec_ref '{ref}' is not defined in specs/core/amk-authority")
            elif r["status"] in ("informative", "reserved", "retired"):
                errs.append(f"{tid}: spec_ref '{ref}' resolves only to {r['status']} text ({r['file']}: {r['title']})")
        for d in t["depends_on"]:
            if d == tid:
                errs.append(f"{tid}: depends on itself")
            elif d not in tasks:
                errs.append(f"{tid}: depends_on unknown task '{d}'")
            elif tasks[d]["status"] == "dropped" and t["status"] != "dropped":
                errs.append(f"{tid}: depends on dropped task {d}; repoint to its replacement "
                            f"({', '.join(tasks[d].get('superseded_by', [])) or 'none recorded'})")
        st = t["status"]
        if st == "blocked" and not t["blocked_by"]:
            errs.append(f"{tid}: blocked requires blocked_by")
        if st == "dropped":
            if not t["dropped_reason"].strip():
                errs.append(f"{tid}: dropped requires dropped_reason")
            if "superseded_by" not in t:
                errs.append(f"{tid}: dropped requires superseded_by (a list; empty if nothing replaces it)")
            for r in t.get("superseded_by", []):
                if r not in tasks or tasks[r]["status"] == "dropped":
                    errs.append(f"{tid}: superseded_by '{r}' must be an existing live task")
        elif t.get("superseded_by"):
            errs.append(f"{tid}: only dropped tasks may set superseded_by")
        if st == "done":
            errs += validate_evidence(tid, t)
            for d in t["deliverables"]:
                if not (ROOT / d).exists():
                    errs.append(f"{tid}: done but deliverable missing: {d}")
        elif t["evidence"]:
            errs.append(f"{tid}: evidence must be empty unless status is done")
    for tid, t in tasks.items():
        if t["status"] in ("in_progress", "done"):
            for d in t["depends_on"]:
                if d in tasks and tasks[d]["status"] != "done":
                    errs.append(f"{tid}: {t['status']} but dependency {d} is {tasks[d]['status']}")
    seen, stack = set(), []

    def dfs(n):
        if n in stack:
            errs.append("dependency cycle: " + " -> ".join(stack[stack.index(n):] + [n]))
            return
        if n in seen or n not in tasks:
            return
        stack.append(n)
        for d in tasks[n]["depends_on"]:
            dfs(d)
        stack.pop()
        seen.add(n)

    for n in tasks:
        dfs(n)
    order = {s["id"]: i for i, s in enumerate(stages)}
    for s in stages:
        if not s.get("gate") or stage_state(tasks, s["id"]) in ("done", "empty"):
            continue
        for tid, t in tasks.items():
            if t["stage"] in order and order[t["stage"]] > order[s["id"]] and t["status"] in ("in_progress", "done"):
                errs.append(f"{tid}: {t['status']} in stage {t['stage']} while gate stage {s['id']} is not complete")
    return stages, tasks, errs


def counts(tasks, sid=None):
    c = {s: 0 for s in STATUSES}
    for t in tasks.values():
        if sid is None or t["stage"] == sid:
            c[t["status"]] += 1
    return c


# ----------------------------------------------------------------- commands
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
           "- **Remove a task:** delete the file only if it was never started. Otherwise set `status` to `dropped` with a `dropped_reason` and a `superseded_by` list (empty if nothing replaces it), and repoint any task that depended on it.",
           "- **Add, reorder or remove a stage:** edit `plan/stages.json`. A stage can only be removed when no task refers to it.",
           "- **Complete a task:** all acceptance statements are met and its verification passes. `set ID done --pr <url> --commit <sha> --by <name>` runs the verification and records the evidence.",
           "- Run `check` before every plan PR. A task cannot be `in_progress` or `done` while a dependency is open, or while an earlier gate stage is incomplete. Run `coverage` to see which normative specification items no task cites.", "",
           "## Statuses", "",
           "| Status | Meaning | Required |",
           "|---|---|---|",
           "| `todo` | Not started | none |",
           "| `in_progress` | Being worked on | dependencies done; earlier gate stages complete |",
           "| `blocked` | Cannot proceed | `blocked_by` (task IDs, open items, or a decision) |",
           "| `done` | Acceptance met and verified | `evidence`: PR URL, commit SHA, `verified_at`, `verified_by`, `verification` (method and pass); deliverables present |",
           "| `dropped` | No longer needed | `dropped_reason`, `superseded_by`; no live task may depend on it |", "",
           "Verification methods: `manual` (reviewer confirms every acceptance statement; `evidence.notes` maps each to evidence), `files` (deliverables exist), `command` (`verify ID` runs `verify.command` from `impl/core/amk-authority`).", ""]
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
    if errs:
        for e in errs:
            print("ERROR:", e)
        print("not rendered: fix the errors above")
        return 1
    if a.check:
        if not PLAN_MD.exists() or PLAN_MD.read_text() != text:
            print("ERROR: PLAN.md is out of date; run `render`")
            return 1
        print("PLAN.md is up to date")
        return 0
    PLAN_MD.write_text(text)
    print(f"wrote {PLAN_MD.relative_to(ROOT)}")
    return 0


def run_verification(t):
    """Run a task's automatic verification. Returns (ok, lines)."""
    v, ok, lines = t["verify"], True, []
    for d in t["deliverables"]:
        exists = (ROOT / d).exists()
        lines.append(f"  deliverable {'OK     ' if exists else 'MISSING'} {d}")
        ok &= exists
    if v["method"] == "command":
        lines.append(f"  $ {v['command']}")
        r = subprocess.run(v["command"], shell=True, cwd=IMPL)
        ok &= r.returncode == 0
    return ok, lines


def cmd_verify(a):
    _, tasks, _ = check()
    t = tasks.get(a.id)
    if not t:
        print(f"ERROR: unknown task {a.id}")
        return 1
    print(f"{a.id}: {t['title']}  [status {t['status']}, method {t['verify']['method']}]")
    ok, lines = run_verification(t)
    print("\n".join(lines))
    if t["verify"]["method"] == "manual":
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
    original = f.read_text()
    t = json.loads(original)
    t["status"] = a.status
    t["evidence"] = {}
    if a.status == "done":
        if not (a.pr and a.commit and a.by):
            print("ERROR: done requires --pr, --commit and --by")
            return 1
        if not PR_RE.match(a.pr) or not SHA_RE.match(a.commit):
            print("ERROR: --pr must be https://github.com/inscope-labs/osx/pull/<n>; --commit must be a 7 to 40 character lowercase hex SHA")
            return 1
        g = subprocess.run(["git", "cat-file", "-e", a.commit + "^{commit}"], cwd=ROOT, capture_output=True)
        if g.returncode != 0:
            print(f"ERROR: commit {a.commit} does not exist in this repository")
            return 1
        method = t["verify"]["method"]
        if method == "manual" and not (a.notes or "").strip():
            print("ERROR: manual verification requires --notes mapping each acceptance statement to its evidence")
            return 1
        if method in ("files", "command"):
            ok, lines = run_verification(t)
            print("\n".join(lines))
            if not ok:
                print("ERROR: verification failed; task not marked done")
                return 1
        verification = {"method": method, "result": "pass"}
        if method == "command":
            verification["command"] = t["verify"]["command"]
        t["evidence"] = {"pr": a.pr, "commit": a.commit,
                         "verified_at": datetime.date.today().isoformat(),
                         "verified_by": a.by, "verification": verification, "notes": a.notes or ""}
    if a.status == "blocked":
        t["blocked_by"] = a.blocked_by or t["blocked_by"]
    if a.status == "dropped":
        t["dropped_reason"] = a.reason or ""
        t["superseded_by"] = a.superseded_by or []
    f.write_text(json.dumps(t, indent=2) + "\n")
    if cmd_check(a) != 0:
        f.write_text(original)
        print("change reverted: the plan would no longer pass `check`")
        return 1
    return 0


def cmd_registry(_):
    for rid, r in sorted(build_registry().items()):
        print(f"{rid:<11} {r['kind']:<10} {r['status']:<11} {r['file']:<24} {r['title']}")
    return 0


def cmd_coverage(a):
    _, tasks, errs = check()
    registry = build_registry()
    exempt = load_exempt()
    bad = [i for i in exempt if i not in registry]
    cited = {r for t in tasks.values() if t["status"] != "dropped" for r in t["spec_refs"]}
    normative = [i for i, r in registry.items() if r["status"] == "normative"]
    uncovered = sorted(i for i in normative if i not in cited and i not in exempt)
    print(f"{len(normative)} normative items; {len(normative) - len(uncovered) - len([i for i in exempt if i in normative and i not in cited])} cited by a live task; "
          f"{len([i for i in exempt if i in normative and i not in cited])} exempt; {len(uncovered)} uncovered")
    for i in uncovered:
        print(f"UNCOVERED {i}  {registry[i]['file']}: {registry[i]['title']}")
    for i in bad:
        print(f"ERROR: coverage_exempt.json lists unknown item {i}")
    return 1 if (a.fail and (uncovered or bad)) or bad else 0


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("check").set_defaults(fn=cmd_check)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    sub.add_parser("registry").set_defaults(fn=cmd_registry)
    cv = sub.add_parser("coverage"); cv.add_argument("--fail", action="store_true"); cv.set_defaults(fn=cmd_coverage)
    r = sub.add_parser("render"); r.add_argument("--check", action="store_true"); r.set_defaults(fn=cmd_render)
    v = sub.add_parser("verify"); v.add_argument("id"); v.set_defaults(fn=cmd_verify)
    s = sub.add_parser("set"); s.add_argument("id"); s.add_argument("status", choices=STATUSES)
    s.add_argument("--pr"); s.add_argument("--commit"); s.add_argument("--by"); s.add_argument("--notes")
    s.add_argument("--reason"); s.add_argument("--superseded-by", nargs="*", dest="superseded_by")
    s.add_argument("--blocked-by", nargs="*", dest="blocked_by")
    s.set_defaults(fn=cmd_set)
    a = p.parse_args()
    sys.exit(a.fn(a))


if __name__ == "__main__":
    main()
