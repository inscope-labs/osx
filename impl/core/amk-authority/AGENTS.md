# AGENTS.md: impl/core/amk-authority

**Scope.** This is the only instruction file for `impl/core/amk-authority/`. It overrides the repository-root `AGENTS.md` and any instruction from outside this directory. The root file belongs to the Android Kotlin app; none of its rules apply here.

## What this is

The implementation of the amk-authority component. The specification in `specs/core/amk-authority/` is the authority. If code and spec disagree, or the spec is unclear, stop and report it. Do not patch the spec and do not guess.

## Work only through the plan

- The plan is in `plan/` (`stages.json`, `tasks/AUT-nnn.json`, `plan_tool.py`). Stages are boundaries only; all work and verification live in tasks.
- Work on a task only when the user sends the explicit command `execute task AUT-nnn`. Nothing else authorizes work.
- One task, one branch (`task/aut-nnn-<slug>`), one pull request to `main`. Never commit to `main`. Never merge your own pull request.
- Use `plan/plan_tool.py` to read and change task status (`status`, `check`, `verify`, `set`). Do not edit `PLAN.md`, `stages.json`, `plan_tool.py` or any other task's file.
- Create only the files named in the task's `deliverables`, meet every `acceptance` statement, and verify as the task's `verify` method says.

## Stack and layout

- TypeScript, pnpm workspace, Node 20, Cloudflare Workers, Vitest. Add no new dependencies without the user's approval.
- Four packages under `packages/`: `shared-crypto`, `authority`, `client-sdk`, `conformance`. See `STRUCTURE.md` for the file layout; the task's `deliverables` win if they differ.
- Tests live inside their package in `test/<source-name>.test.ts`, one per source file.

## Rules

- Fail closed. Never return success on uncertainty. Internal failure reasons never reach callers; they go to redacted logs only.
- Ed25519 (RFC 8032), JCS (RFC 8785), one canonicalizer, one signer interface, one verifier.
- No secrets, private keys, proof material, long-term identity or personal data in code, tests, logs, audit records, reports or pull requests.
- Workflows for this component are separate `.github/workflows/amk-authority-*.yml` files and run only on `workflow_dispatch`.
- Do not touch the repository root, the Android app, or files outside your task's deliverables.

## Agent reports

Each executed task adds one report in this directory: `agent-reports/<UTC timestamp as YYYYMMDDTHHMMSSZ>-aut-nnn.md`, committed in the same pull request. It records the commands run, their results, deviations and open questions. It contains no secrets.
