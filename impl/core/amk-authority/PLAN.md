# amk-authority implementation plan

> **Generated file. Do not edit.** Edit `plan/stages.json` or `plan/tasks/AUT-nnn.json`, then run `python3 impl/core/amk-authority/plan/plan_tool.py render`.

Stages are boundaries only. All work and all verification live in atomic tasks. A stage's state is derived from its tasks and is never stored.

**Tasks:** 60 total · todo 60 · in_progress 0 · blocked 0 · done 0 · dropped 0

## How to change this plan

- **Add a task:** create `plan/tasks/AUT-nnn.json` with the next free number (IDs are never reused or reassigned) and fill every field.
- **Adjust a task:** edit its file. Moving it to another stage is a one-field change (`stage`).
- **Remove a task:** delete the file only if it was never started. Otherwise set `status` to `dropped` with a `dropped_reason`, so history stays traceable.
- **Add, reorder or remove a stage:** edit `plan/stages.json`. A stage can only be removed when no task refers to it.
- **Complete a task:** all acceptance statements are met, its verification passes, and `status` is `done` with `evidence.pr` or `evidence.commit` and `evidence.verified_at`. The `set` command writes this for you.
- Run `check` before every plan PR. A task cannot be `in_progress` or `done` while a dependency is open, or while an earlier gate stage is incomplete.

## Statuses

| Status | Meaning | Required |
|---|---|---|
| `todo` | Not started | none |
| `in_progress` | Being worked on | dependencies done or dropped; earlier gate stages complete |
| `blocked` | Cannot proceed | `blocked_by` (task IDs, open items, or a decision) |
| `done` | Acceptance met and verified | `evidence.pr` or `evidence.commit`, `evidence.verified_at`, deliverables present |
| `dropped` | No longer needed | `dropped_reason` |

Verification methods: `manual` (reviewer confirms every acceptance statement in the PR), `files` (deliverables exist), `command` (`verify ID` runs `verify.command` from `impl/core/amk-authority`).

## S0: Decisions and scaffold  (gate)

**State:** todo (0/11 done)

**Goal:** Settle the specification decisions that block Basic, and create the project skeleton.

**Exit criteria:** All S0 tasks done: blocking Basic decisions recorded in the specification, AGENTS.md local to this directory in place, and an empty Worker deployed manually.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-001` | Decide signature algorithm and canonical serialization | todo | - | manual | ATA-01:02, ATA-01:05 |
| `AUT-002` | Resolve role list format and how it reaches the Authority | todo | - | manual | ATA-02:04, O-ATA-02 |
| `AUT-003` | Resolve enrollment of controller and router at first use | todo | - | manual | O-ATA-05 |
| `AUT-004` | Resolve revocation distribution and unknown-status behavior for Basic | todo | - | manual | ATA-04:01, ATA-03:05, O-ATA-04 |
| `AUT-005` | Resolve signing key rotation scope for Basic | todo | - | manual | O-ATA-01 |
| `AUT-006` | Specify who may request revocation and how they authenticate | todo | - | manual | ATA-04:01, ATA-04:02 |
| `AUT-007` | Create AGENTS.md local to impl/core/amk-authority | todo | - | files | - |
| `AUT-008` | Scaffold the TypeScript project | todo | AUT-007 | files | - |
| `AUT-009` | Create the Worker and Authority Durable Object skeleton | todo | AUT-008 | command | - |
| `AUT-010` | Add manual-dispatch workflows for test and deploy | todo | AUT-009 | files | - |
| `AUT-011` | Deploy the empty Worker manually | todo | AUT-010 | manual | - |

## S1: Attestation core

**State:** todo (0/7 done)

**Goal:** Implement the attestation format, canonical serialization, signing, validity and the shared test vectors.

**Exit criteria:** Vector set v1 passes in CI, including malformed, expired, tampered and unknown-algorithm cases.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-012` | Attestation type and field validation | todo | AUT-008, AUT-001 | command | ATA-01:01 |
| `AUT-013` | Canonical serialization | todo | AUT-012 | command | ATA-01:02 |
| `AUT-014` | Signing and verification primitives | todo | AUT-013 | command | ATA-01:05 |
| `AUT-015` | Validity window and grace handling | todo | AUT-012 | command | ATA-01:03, DV-ATA-01, DV-ATA-03, DV-ATA-07 |
| `AUT-016` | Nonce generation and format | todo | AUT-012 | command | ATA-01:04 |
| `AUT-017` | Publish test vector set v1 | todo | AUT-013, AUT-014, AUT-015 | files | ATA-01:06 |
| `AUT-018` | Run vectors in CI | todo | AUT-017, AUT-010 | command | - |

## S2: Issuance (Basic)

**State:** todo (0/8 done)

**Goal:** Implement issuance with proof of possession, role validation, replay protection and enrollment.

**Exit criteria:** A test client obtains a valid attestation; bad proof of possession, replayed nonces and out-of-list roles are rejected.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-019` | Issuance request schema and validation | todo | AUT-012, AUT-002 | command | ATA-02:01, ATA-02:02, DV-ATA-04 |
| `AUT-020` | Proof-of-possession challenge and response | todo | AUT-019, AUT-014 | command | ATA-02:03, DV-ATA-05, O-ATA-07 |
| `AUT-021` | Role validation against the declared role list | todo | AUT-019, AUT-002 | command | ATA-02:04, O-ATA-02 |
| `AUT-022` | Issuance nonce replay protection in the Durable Object | todo | AUT-009, AUT-019 | command | ATA-02:02, DV-ATA-04 |
| `AUT-023` | Issuance and binding rules | todo | AUT-014, AUT-020, AUT-021, AUT-022 | command | ATA-02:06, ATA-02:07 |
| `AUT-024` | Enrollment flow for controller and router | todo | AUT-003, AUT-009 | command | O-ATA-05 |
| `AUT-025` | Signing key storage and loading | todo | AUT-014, AUT-005 | command | ATA-00:04 |
| `AUT-026` | Issuance endpoint end-to-end test | todo | AUT-023, AUT-024, AUT-025 | command | - |

## S3: Verification and revocation (Basic)

**State:** todo (0/8 done)

**Goal:** Provide the verifier library and the revocation list, failing closed everywhere.

**Exit criteria:** The verifier rejects every unverifiable case and revocation takes effect within the published interval.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-027` | Verifier library | todo | AUT-017, AUT-023 | command | ATA-03:01, ATA-03:02, ATA-03:03 |
| `AUT-028` | Nonce freshness and session context verification | todo | AUT-027 | command | ATA-03:04, ATA-03:06 |
| `AUT-029` | Revocation list structure and signing | todo | AUT-014, AUT-004 | command | ATA-04:01, ATA-04:02 |
| `AUT-030` | Revocation write path in the Durable Object | todo | AUT-006, AUT-022, AUT-029 | command | ATA-04:01 |
| `AUT-031` | Revocation list pull endpoint | todo | AUT-030 | command | ATA-04:01, DV-ATA-06 |
| `AUT-032` | Client obligations in the verifier | todo | AUT-027, AUT-029, AUT-004 | command | ATA-04:03, ATA-03:05, DV-ATA-02 |
| `AUT-033` | Router and factory behavior in the verifier | todo | AUT-027 | command | ATA-03:07 |
| `AUT-034` | Fail-closed negative suite for the verifier | todo | AUT-028, AUT-032, AUT-033 | command | - |

## S4: Integration and Basic conformance  (gate)

**State:** todo (0/6 done)

**Goal:** Prove the Basic profile end to end against controller and router stand-ins and cut a Basic candidate.

**Exit criteria:** Basic conformance suite green, deviations documented, candidate tagged.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-035` | Controller and router stand-in harness | todo | AUT-026, AUT-034 | files | - |
| `AUT-036` | End-to-end integration test | todo | AUT-035 | command | - |
| `AUT-037` | Basic conformance suite with rule traceability | todo | AUT-036 | command | ATA-06:02 |
| `AUT-038` | Update amk-test-matrix and regenerate spec-full | todo | AUT-037 | manual | - |
| `AUT-039` | Declare Basic profile metadata | todo | AUT-037 | files | ATA-06:05 |
| `AUT-040` | Tag Basic candidate | todo | AUT-038, AUT-039 | manual | - |

## S5: Standard profile

**State:** todo (0/6 done)

**Goal:** Add non-exportable keys, real-time revocation, rotation and metadata publication.

**Exit criteria:** Standard conformance suite green on the chosen key host.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-041` | Decide the Standard key host | todo | AUT-040 | manual | ATA-06:03 |
| `AUT-042` | Implement the non-exportable key provider | todo | AUT-025, AUT-041 | command | ATA-06:03 |
| `AUT-043` | Real-time revocation endpoint | todo | AUT-031, AUT-040 | command | ATA-04:04, ATA-04:05 |
| `AUT-044` | Multiple signing keys and rotation | todo | AUT-042 | command | O-ATA-01 |
| `AUT-045` | Metadata endpoint | todo | AUT-040 | command | O-ATA-08, ATA-06:05 |
| `AUT-046` | Standard conformance suite | todo | AUT-042, AUT-043, AUT-044, AUT-045 | command | ATA-06:03 |

## S6: Identity binding and audit

**State:** todo (0/5 done)

**Goal:** Implement identity binding and the signed audit trail.

**Exit criteria:** Binding and audit conformance tests green for the profiles that require them.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-047` | Resolve identity binding scope | todo | AUT-040 | manual | ATA-05:04, O-ATA-03 |
| `AUT-048` | Resolve audit format and retention | todo | AUT-040 | manual | ATA-05:05, O-ATA-09 |
| `AUT-049` | Identity binding process | todo | AUT-047, AUT-023 | command | ATA-05:03, ATA-05:04 |
| `AUT-050` | Signed audit trail | todo | AUT-048, AUT-023, AUT-030 | command | ATA-05:05 |
| `AUT-051` | Binding and audit tests | todo | AUT-049, AUT-050 | command | - |

## S7: High profile

**State:** todo (0/5 done)

**Goal:** Add hardware-backed keys and a verifiable High claim.

**Exit criteria:** High conformance suite green, profile claim independently verifiable.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-052` | Decide the High key host and how the High claim is verified | todo | AUT-046 | manual | ATA-06:04 |
| `AUT-053` | Implement the hardware-backed key provider | todo | AUT-052, AUT-042 | command | ATA-06:04 |
| `AUT-054` | Remote attestation of the Authority | todo | AUT-052 | command | ATA-06:04 |
| `AUT-055` | Enforce mandatory real-time revocation | todo | AUT-043, AUT-052 | command | ATA-04:04, ATA-06:04 |
| `AUT-056` | High conformance suite | todo | AUT-053, AUT-054, AUT-055, AUT-050 | command | ATA-06:04 |

## S8: Release hardening

**State:** todo (0/4 done)

**Goal:** Run full conformance per profile, regenerate specifications and release.

**Exit criteria:** Release tagged with deviations and unsupported features documented.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-057` | Full conformance run per profile | todo | AUT-046, AUT-051, AUT-056 | manual | - |
| `AUT-058` | Regenerate specifications | todo | AUT-057 | manual | - |
| `AUT-059` | Release | todo | AUT-058 | manual | - |
| `AUT-060` | Review and close the plan | todo | AUT-059 | manual | - |

---

`E-ATA-nn` identifiers in task `findings` refer to the cursory review findings in the amk-authority specification and are informational only.
