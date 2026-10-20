# Operator guide: worker and supervisor method

Two prompts, one loop. You move files between them; neither agent needs the other's tools. The plan tool is the only source of "what can run next". Do not copy its output into this file, because it goes stale on the first merge.

| Role | Prompt | Needs |
|---|---|---|
| Agentic Worker | `AGENT-WORKER-PROMPT.md` | Nothing but the files you attach. No git, no token. |
| Agentic Supervisor | `AGENT-SUPERVISOR-PROMPT.md` | Shell, git, python3, node 20 and pnpm, GitHub access with a scoped token. |

## Asking the plan tool

Without a clone, run the **Plan tool** workflow (Actions, manual). Its default config `plan/run.config.json` runs `check`, `status` and `next`; edit or supply another config to run `order AUT-nnn ...`. Pass `assume_done` (comma-separated task IDs) to see what would become orderable once those tasks merge. It is read-only: mutating commands are refused. With a clone, run the same commands directly:

```
python3 impl/core/amk-authority/plan/plan_tool.py next [--assume-done AUT-007,AUT-008]
python3 impl/core/amk-authority/plan/plan_tool.py order AUT-007 [AUT-008 ...]
```

## The loop

1. **Choose the next order.** Use the latest `EXIT-REPORT.md` ("Next orders") or `next`.
2. **Generate the TASK ORDER block** with `order`. Add `--base-url HEAD` (or a commit SHA) when the worker will fetch files instead of receiving attachments: the block then lists every file as a raw URL pinned to that commit, directories expanded, and the command refuses if any listed file differs from your working tree. Push the commit to GitHub before you give the order to the worker, or the URLs will not load. Individual, grouped and stage orders use the same command. It refuses unsatisfied dependencies, closed gates and operator-only tasks.
3. **Brief the worker.** Fresh conversation, one order per conversation. Give it `AGENT-WORKER-PROMPT.md` and paste the TASK ORDER block exactly as printed, nothing else. Without `--base-url`, also attach every file under `package:` and `context (read-only):`. With it, the worker must be able to fetch URLs. Never edit the block (the worker blocks on a missing key or an unpinned link).
4. **Collect the result.** The first lines of the reply are one of:
   - `READY_FOR_SUPERVISOR`: every task delivered.
   - `PARTIAL_FOR_SUPERVISOR`: some tasks delivered, the rest blocked (the `blocked:` list says why).
   - `BLOCKED_*`: nothing produced. Answer the question or fix the input, then send a new order.

   For the first two, save each `=== FILE ... ===` block under the `upload_to` folder (or take the worker's zip). Keep the layout exactly.
5. **Upload** the whole folder `impl/core/amk-authority/submissions/<submission_id>/` to `main`, unchanged.
6. **Brief the supervisor.** Give it `AGENT-SUPERVISOR-PROMPT.md` with the token pasted in, then send the single line `SUBMISSION: <submission_id>`.
7. **Review the pull request.** The body lists each acceptance statement with its `path:lines`, and any replaced files. Merge it if you agree. The supervisor never merges.
8. **Read the exit report** (printed and committed in the submission folder), then go to step 1. For `RETURNED` tasks it contains a rework order; resubmit with the suffix it gives (`-r2`, `-r3`). For `BLOCKED` tasks, answer "Decisions needed from the operator" first.

## Individual, group or stage orders

The exit report recommends one and lists alternatives, computed by the plan tool:
- **Individual:** one ready task. Use for risky or unclear work.
- **Group:** several ready tasks in one stage that do not depend on each other.
- **Stage:** every worker task of a stage that can be ordered together, in dependency order. Use for well-understood work.

A blocked task no longer discards the whole order: the worker delivers the independent tasks and marks the others blocked (`PARTIAL_FOR_SUPERVISOR`). Missing input still blocks the whole order.

## Work already on main

If a task's deliverable was placed by hand (for example `AGENTS.md`), do not order it: the supervisor will not accept an identical file. Record it yourself in a clone with `plan_tool.py set <id> done --pr <PR URL> --commit <sha> --by "<you>"` (the tool requires a real pull request URL and an existing commit), then run `render` and `check` and merge that change by pull request.

## Operator-only tasks

These need you (decisions, merges, workflows, deployments, drills). The tool refuses to order them. List them with `plan_tool.py next` (executor `operator`) rather than from memory.

## Token for the supervisor

Create a fine-grained token for `inscope-labs/osx` only, with **Contents: read and write** and **Pull requests: read and write** (Metadata read is automatic), nothing else, expiring in a few days. GitHub cannot limit a token to branches, so protect `main` with a ruleset that requires pull requests. Pushing under `.github/workflows` additionally needs the **Workflows** permission; the supervisor must not have it. Revoke the token when the session ends, and never paste it into a worker conversation.

## What replaced what

The three lane prompts (A, B, C), the qualification prompts and the earlier worker prompt are superseded and kept under `agent-prompts/superseded/`. Lanes no longer exist: any worker can take any worker task, because every order is self-contained.
