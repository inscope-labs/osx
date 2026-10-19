#!/usr/bin/env python3
"""Read a plan_tool command config and produce a Markdown response.

Used by .github/workflows/plan-tool.yml. Only read-only subcommands are
accepted — mutating subcommands (`set`, bare `render`) are rejected because
CI does not commit anything back.
"""
import argparse, json, os, subprocess, sys
from pathlib import Path

READ_ONLY = {"check", "status", "registry", "coverage", "next", "order", "verify"}


def is_read_only(argv):
    if not argv:
        return False
    cmd = argv[0]
    if cmd == "render":
        return "--check" in argv[1:]
    return cmd in READ_ONLY


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--config", required=True)
    ap.add_argument("--plan-tool", required=True)
    ap.add_argument("--assume-done", default="")
    ap.add_argument("--out", required=True)
    a = ap.parse_args()

    cfg_path = Path(a.config)
    if not cfg_path.is_file():
        sys.exit(f"ERROR: config not found: {cfg_path}")
    cfg = json.loads(cfg_path.read_text())
    cmds = cfg.get("commands", [])
    if not isinstance(cmds, list) or not cmds:
        sys.exit(f"ERROR: {cfg_path} must contain a non-empty 'commands' list")

    assume = (a.assume_done or "").replace(" ", "").strip(",")

    lines = [
        "# Plan tool response",
        "",
        f"- config: `{a.config}`",
        f"- ref: `{os.environ.get('GITHUB_SHA', 'local')}`",
        f"- workflow run: {os.environ.get('GITHUB_RUN_ID', 'local')}",
        f"- commands: {len(cmds)}",
        "",
    ]
    failed = 0
    for i, c in enumerate(cmds, 1):
        args = c.get("args") if isinstance(c, dict) else None
        if not isinstance(args, list) or not args:
            lines += [f"## Command {i}: INVALID", "", "```", "missing or invalid 'args' list", "```", ""]
            failed += 1
            continue
        if not is_read_only(args):
            lines += [
                f"## Command {i}: `{' '.join(args)}`",
                "",
                "```",
                "rejected: mutating subcommands are not permitted in this workflow",
                "```",
                "",
            ]
            failed += 1
            continue
        argv = list(args)
        if argv[0] == "next" and assume:
            argv += ["--assume-done", assume]
        lines += [f"## Command {i}: `plan_tool.py {' '.join(argv)}`", "", "```"]
        r = subprocess.run([sys.executable, a.plan_tool, *argv], capture_output=True, text=True)
        lines.append((r.stdout or "") + (r.stderr or ""))
        lines += ["```", "", f"_exit code: {r.returncode}_", ""]
        if r.returncode != 0:
            failed += 1

    Path(a.out).write_text("\n".join(lines))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
