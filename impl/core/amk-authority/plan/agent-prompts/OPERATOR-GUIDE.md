# Operator guide: worker and supervisor method

Two prompts, one loop. You move files between them; neither agent needs the other's tools.

| Role | Prompt | Needs |
|---|---|---|
| Agentic Worker | `AGENT-WORKER-PROMPT.md` | Nothing but the files you attach. No git, no token. |
| Agentic Supervisor | `AGENT-SUPERVISOR-PROMPT.md` | Shell, git, python3, node 20 and pnpm, GitHub access with a scoped token. |

## The loop

1. **Choose the next order.** Use the latest `EXIT-REPORT.md` ("Next orders"), or run in a clone:
   `python3 impl/core/amk-authority/plan/plan_tool.py next`
2. **Generate the TASK ORDER block.** Individual, grouped or stage orders all use the same command with the task IDs from the report:
   `python3 impl/core/amk-authority/plan/plan_tool.py order AUT-007 [AUT-008 ...]`
   It refuses an order whose dependencies or gates are not satisfied, and refuses operator-only tasks.
3. **Brief the worker.** Start a fresh worker conversation (one order per conversation). Give it `AGENT-WORKER-PROMPT.md`, attach every file listed under `package:` and `context (read-only):`, and paste the TASK ORDER block. Nothing else.
4. **Collect the result.** If the first line says `READY_FOR_SUPERVISOR`, save each `=== FILE ... ===` block under the `upload_to` folder, or take the worker's zip. Keep the layout exactly. If it says `BLOCKED_*`, answer the question (or fix the input) and send a new order; nothing was produced.
5. **Upload** the whole folder `impl/core/amk-authority/submissions/<submission_id>/` to `main`, unchanged.
6. **Brief the supervisor.** Give it `AGENT-SUPERVISOR-PROMPT.md` with the token pasted in, then send the single line `SUBMISSION: <submission_id>`.
7. **Review the pull request** the supervisor opens: the body lists each acceptance statement with its `path:lines`. Merge it if you agree. The supervisor never merges.
8. **Read the exit report** (printed and committed in the submission folder) and go to step 1. If tasks were `RETURNED`, it contains a rework order; re-submit with the suffix it gives (`-r2`, `-r3`).

## Individual, grouped or stage submissions

The exit report recommends one and lists alternatives, computed by the plan tool, not guessed:
- **Individual:** one ready task.
- **Group:** several ready tasks in the same stage that do not depend on each other.
- **Stage:** every worker task of a stage that can be ordered together, in dependency order.
Larger orders save round trips but a single blocked task blocks the whole order and produces no files, so use stage orders for well-understood work and individual orders for risky tasks.

## Current state (from the plan tool)

```
assumed done: none
READY (each can be ordered on its own)
  AUT-007  S0  [worker] verify=files    Create AGENTS.md local to impl/core/amk-authority
  AUT-061  S0  [operator] verify=manual   Land the standards-based specification resolutions
GROUPS (ready worker tasks in the same stage; independent of each other)
  none
STAGE-ELIGIBLE (worker tasks of a stage that can be ordered together, in this order)
  S0: AUT-007, AUT-008, AUT-063, AUT-009, AUT-064, AUT-010  (operator tasks remaining: AUT-011, AUT-061)
```

Example order:

```
TASK ORDER
submission_id: AUT-007-create-agents-md-local-to-impl-core-amk
mode: individual
tasks: AUT-007
task_dirs:
  - AUT-007: AUT-007-create-agents-md-local-to-impl-core-amk
package:
  - specs/core/amk-authority/
  - impl/core/amk-authority/AGENTS.md
  - impl/core/amk-authority/PLAN.md
  - impl/core/amk-authority/plan/tasks/AUT-007.json
context (read-only):
  - impl/core/amk-authority/plan/stages.json
upload_to: impl/core/amk-authority/submissions/AUT-007-create-agents-md-local-to-impl-core-amk/
```

## Operator-only tasks

These need you (decisions, merges, workflows, deployments, drills). They cannot be given to a worker and the tool refuses to order them:
AUT-011, AUT-038, AUT-040, AUT-041, AUT-057, AUT-058, AUT-059, AUT-060, AUT-061, AUT-062, AUT-078, AUT-079, AUT-081, AUT-084, AUT-086, AUT-088

## Token for the supervisor

Create a fine-grained token for `inscope-labs/osx` only, with **Contents: read and write** and **Pull requests: read and write** (Metadata read is automatic), nothing else, expiring in a few days. GitHub cannot limit a token to branches, so protect `main` with a ruleset that requires pull requests. Revoke the token when the session ends.

## What replaced what

The three lane prompts (A, B, C) and the qualification prompts are superseded. Lanes no longer exist: any worker can take any worker task, because every order is self-contained.
