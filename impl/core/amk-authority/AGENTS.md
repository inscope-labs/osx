# AGENTS.md: impl/core/amk-authority

**Scope.** This is the only instruction file for `impl/core/amk-authority/`. It overrides the repository-root `AGENTS.md` (which belongs to the Android Kotlin app) and any instruction from outside this directory.

## Roles

- **Operator:** the human. Orders work, uploads submissions, reviews and merges pull requests.
- **Agentic Worker:** turns one TASK ORDER into deliverable files and a report. Needs no repository access. Never touches git, task status or the plan.
- **Agentic Supervisor:** receives an uploaded submission, runs the gates, creates the branch and pull request, records status, and writes the exit report. Never edits a worker's deliverable. Never merges.

## What a worker may read (closed list)

1. `specs/core/amk-authority/` (the specification directory)
2. this file
3. `impl/core/amk-authority/PLAN.md`
4. `impl/core/amk-authority/plan/tasks/AUT-nnn.json` for each ordered task
5. read-only context files named in the TASK ORDER

Nothing else exists for a worker. If something needed is missing, the worker returns `BLOCKED_MISSING_INPUT`.

## Engineering rules (binding on every deliverable)

- Stack: TypeScript, pnpm workspace, Node 20, Cloudflare Workers, Vitest. Four packages under `packages/`: `shared-crypto`, `authority`, `client-sdk`, `conformance`. **No new dependencies.**
- Conventions: `strict: true`; ES modules; named exports only; no `any`; explicit return types on exported functions; 2-space indent; LF; UTF-8; final newline; kebab-case file names.
- Fail closed. Never return success on uncertainty. Internal failure reasons never reach callers (opaque denial with a correlation ID); they go to redacted logs only.
- One canonicalizer (JCS, RFC 8785), one signer interface, one verifier. Ed25519 (RFC 8032). Never reimplement them in another file.
- No verification verdict, signature result or revocation status is cached between calls.
- No secrets, private keys, proof material, long-term identity or personal data in code, tests, fixtures, logs, audit records or reports.
- Follow the cited specification rules literally. The specification is the authority.

**Companion tests.** For every deliverable at `.../packages/<pkg>/src/<path>.ts` there MUST also be a test at `.../packages/<pkg>/test/<path>.test.ts` (replace `/src/` with `/test/` and `.ts` with `.test.ts`). Deliverables that are already tests, or are SQL, JSON or Markdown, have no companion. Companion tests are the only files allowed in addition to a task's `deliverables`. Each test name starts with `AC<n>:` where n is the 1-based position of the acceptance statement it covers, and every acceptance statement is covered by at least one test.

## Submission format

```
impl/core/amk-authority/submissions/<submission_id>/
  SUBMISSION.json
  <task_dir>/                 one folder per task, named in the TASK ORDER (task_dirs)
    REPORT.md
    files/<repo-relative path>   one file per deliverable, at its exact final path
```

`SUBMISSION.json` uses schema `amk-authority-submission/1` (keys: schema, submission_id, mode, created_utc, worker, result, tasks with id, dir, deliverables, companion_files, not_run). `REPORT.md` has exactly these headings, in order: Task, Inputs read, Deliverables, Acceptance, Spec rules applied, Verification, Deviations, Open questions, Not done.

The operator uploads the submission folder to `main` unchanged. The supervisor moves each `files/**` entry to its final path on a branch and keeps `SUBMISSION.json`, each `REPORT.md` and the `EXIT-REPORT.md` in the folder as the record.

## Gates (run by the supervisor, in order, stop at the first failure per task)

G0 intake · G1 layout and secrets · G2 report completeness · G3 acceptance and specification review · G4 mechanical verification · G5 branch and pull request · G6 plan integrity · G7 exit report.

## Never

- Commit to `main`, merge, or approve your own pull request.
- Change `PLAN.md`, `stages.json`, `plan_tool.py`, task files or the specification, except the supervisor recording status through `plan_tool.py` and regenerating `PLAN.md`, and a task whose deliverables are specification files.
- Touch the repository root, the Android app, or any file outside a task's deliverables and companion tests.
- Put secrets or tokens anywhere.
