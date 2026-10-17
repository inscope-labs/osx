# amk-authority implementation plan

> **Generated file. Do not edit.** Edit `plan/stages.json` or `plan/tasks/AUT-nnn.json`, then run `python3 impl/core/amk-authority/plan/plan_tool.py render`.

Stages are boundaries only. All work and all verification live in atomic tasks. A stage's state is derived from its tasks and is never stored.

**Tasks:** 88 total · todo 73 · in_progress 0 · blocked 5 · done 0 · dropped 10

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

## S0: Spec landing and project foundation  (gate)

**State:** todo (0/8 done)

**Goal:** Land the standards-based specification, then create the project skeleton, toolchain and manual workflows.

**Exit criteria:** Spec PR merged, AGENTS.md local to this directory in place, toolchain and bindings configured, an empty Worker deployed manually.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-001` | Decide signature algorithm and canonical serialization | dropped | - | manual | - |
| `AUT-002` | Resolve role list format and how it reaches the Authority | dropped | - | manual | - |
| `AUT-003` | Resolve enrollment of controller and router at first use | dropped | - | manual | - |
| `AUT-004` | Resolve revocation distribution and unknown-status behavior for Basic | dropped | - | manual | - |
| `AUT-005` | Resolve signing key rotation scope for Basic | dropped | - | manual | - |
| `AUT-006` | Specify who may request revocation and how they authenticate | dropped | - | manual | - |
| `AUT-007` | Create AGENTS.md local to impl/core/amk-authority | todo | - | files | - |
| `AUT-008` | Scaffold the pnpm workspace with four packages | todo | AUT-007 | files | - |
| `AUT-009` | Create the Worker and Authority Durable Object skeleton | todo | AUT-008, AUT-063 | command | - |
| `AUT-010` | Add manual-dispatch workflows for test and deploy | todo | AUT-063, AUT-064 | files | - |
| `AUT-011` | Deploy the empty Worker manually | todo | AUT-010, AUT-061 | manual | - |
| `AUT-061` | Land the standards-based specification resolutions | todo | - | manual | ATA-99:01, ATA-99:02 |
| `AUT-063` | Configure the toolchain | todo | AUT-008 | command | - |
| `AUT-064` | Configure Wrangler bindings for D1, R2, KV and the Durable Object | todo | AUT-009 | files | - |

## S1: Keys, serialization, metadata and storage

**State:** todo (0/13 done)

**Goal:** Build the data model, canonical serialization, key handling, signed metadata and the audit writer that everything else depends on.

**Exit criteria:** Vectors pass in Node and Workers, metadata verifies against a pinned root key, audit chain verifies.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-012` | Attestation type and field validation | todo | AUT-008, AUT-063 | command | ATA-01:01 |
| `AUT-013` | Canonical serialization (JCS, RFC 8785) | todo | AUT-012 | command | ATA-01:02 |
| `AUT-014` | Ed25519 signing, verification and key identifiers | todo | AUT-013 | command | ATA-01:05, ATA-01:07 |
| `AUT-015` | Validity window rules | todo | AUT-012 | command | ATA-01:03, ATA-03:03, DV-ATA-01, DV-ATA-03, DV-ATA-07 |
| `AUT-016` | Attestation nonce generation | todo | AUT-012 | command | ATA-01:04 |
| `AUT-017` | Publish test vector set v1 | todo | AUT-013, AUT-014, AUT-015 | files | ATA-01:06 |
| `AUT-018` | Run vectors in the manual CI workflow | todo | AUT-017, AUT-010 | command | - |
| `AUT-025` | Signer interface and software signer | todo | AUT-014 | command | ATA-01:05, ATA-06:02 |
| `AUT-045` | Signed metadata document and endpoint | todo | AUT-025, AUT-064 | command | ATA-01:07, DV-ATA-08, O-ATA-08 |
| `AUT-065` | D1 migrations for the data model | todo | AUT-064 | files | ATA-01:01, ATA-02:02, INV-ATA-03 |
| `AUT-066` | Indexes and unique constraints as atomic guards | todo | AUT-065 | files | ATA-02:02, ATA-02:07 |
| `AUT-067` | Metadata publication and key rotation | todo | AUT-045 | command | ATA-01:07, O-ATA-01 |
| `AUT-068` | Audit writer with hash chain and redaction | todo | AUT-065, AUT-013 | command | ATA-06:10, INV-ATA-03 |

## S2: Issuance (Basic)

**State:** todo (0/10 done)

**Goal:** Issue attestations from controller-supplied session records, with proof of possession and replay protection.

**Exit criteria:** A test client obtains a valid attestation; every deny vector is rejected opaquely.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-019` | Issuance request schema and validation | todo | AUT-012, AUT-024 | command | ATA-02:01, ATA-02:02, DV-ATA-04 |
| `AUT-020` | Deterministic proof of possession | todo | AUT-019, AUT-014 | command | ATA-02:03, INV-ATA-01, O-ATA-07 |
| `AUT-021` | Role validation against the session record | todo | AUT-019, AUT-024 | command | ATA-02:04, INV-ATA-02, O-ATA-02 |
| `AUT-022` | Request nonce replay protection | todo | AUT-066, AUT-019 | command | ATA-02:02, DV-ATA-04 |
| `AUT-023` | Attestation assembly, signing and binding rules | todo | AUT-020, AUT-021, AUT-022, AUT-025, AUT-045 | command | ATA-02:06, ATA-02:07, ATA-01:03, INV-ATA-06 |
| `AUT-024` | Controller identity pinning and session record intake | todo | AUT-062, AUT-066, AUT-068 | command | ATA-02:02, O-ATA-05 |
| `AUT-026` | Issuance end-to-end test | todo | AUT-070, AUT-024 | command | - |
| `AUT-062` | Resolve O-ATA-11: controller-to-Authority interface | todo | AUT-061 | manual | O-ATA-11, ATA-02:02, ATA-04:02 |
| `AUT-069` | Opaque error mapping with correlation ID | todo | AUT-019 | command | INV-ATA-04, ATA-02:02 |
| `AUT-070` | Issuance endpoint | todo | AUT-023, AUT-069, AUT-068 | command | ATA-02:01, ATA-02:02 |

## S3: Verification and client SDK

**State:** todo (0/7 done)

**Goal:** Implement verification once, reused by the controller and any client.

**Exit criteria:** The SDK rejects every unverifiable case and exposes only an opaque verdict.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-027` | Verifier pipeline | todo | AUT-071, AUT-015 | command | ATA-03:01, ATA-03:02, ATA-03:03 |
| `AUT-028` | Presentation proof and session context verification | todo | AUT-027 | command | ATA-03:04, ATA-03:06, DV-ATA-05 |
| `AUT-033` | Router and factory behavior in the verifier | dropped | - | manual | - |
| `AUT-034` | Fail-closed negative suite for the verifier | todo | AUT-028, AUT-032, AUT-073 | command | - |
| `AUT-071` | SDK metadata resolver | todo | AUT-017, AUT-045 | command | ATA-01:07, ATA-03:02, DV-ATA-08 |
| `AUT-072` | Pluggable verifier nonce store | todo | AUT-028, AUT-066 | command | ATA-03:04 |
| `AUT-073` | Peer profile rule in the SDK | todo | AUT-071 | command | ATA-06:07, ATA-06:05 |
| `AUT-074` | Controller integration contract and failure-log hook | todo | AUT-027 | files | ATA-03:07 |

## S4: Revocation

**State:** todo (0/4 done)

**Goal:** Publish a signed pull-only revocation list and a controller-authorized revocation path.

**Exit criteria:** Revocation takes effect within the stated worst case and clients fail closed.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-029` | Revocation list structure and signing | todo | AUT-014, AUT-045 | command | ATA-04:01 |
| `AUT-030` | Revocation request path | todo | AUT-062, AUT-029, AUT-068 | command | ATA-04:02, ATA-06:10 |
| `AUT-031` | Revocation list publication | todo | AUT-030 | command | ATA-04:01, DV-ATA-06 |
| `AUT-032` | SDK revocation handling | todo | AUT-027, AUT-029 | command | ATA-03:05, ATA-04:03, DV-ATA-02, INV-ATA-05 |

## S5: Basic integration and conformance  (gate)

**State:** todo (0/7 done)

**Goal:** Prove Basic end to end against a controller stand-in and cut a Basic candidate.

**Exit criteria:** Basic conformance suite green, retention jobs running, candidate tagged.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-035` | Controller stand-in harness | todo | AUT-026, AUT-034 | files | - |
| `AUT-036` | End-to-end integration test | todo | AUT-035, AUT-031, AUT-032 | command | - |
| `AUT-037` | Basic conformance suite with rule traceability | todo | AUT-036 | command | ATA-06:09 |
| `AUT-038` | Update amk-test-matrix and regenerate spec-full | todo | AUT-037 | manual | - |
| `AUT-039` | Basic profile metadata | todo | AUT-037 | files | ATA-06:05, ATA-06:09 |
| `AUT-040` | Tag the Basic candidate | todo | AUT-038, AUT-039, AUT-075 | manual | - |
| `AUT-044` | Multiple signing keys and rotation | dropped | - | manual | - |
| `AUT-075` | Retention jobs | todo | AUT-031, AUT-068 | command | DV-ATA-10, DV-ATA-04, ATA-06:10 |

## S6: Standard profile

**State:** todo (0/6 done)

**Goal:** Add a real non-exportable key host, requester authentication, real-time revocation and profile enforcement.

**Exit criteria:** Standard conformance suite green; Standard refuses to start with software keys.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-041` | Decide the Standard key host | todo | AUT-040 | manual | ATA-06:03 |
| `AUT-042` | ProxySigner against the chosen key host | todo | AUT-025, AUT-041 | command | ATA-06:03 |
| `AUT-043` | Real-time revocation endpoint | todo | AUT-031, AUT-042 | command | ATA-04:04, DV-ATA-09 |
| `AUT-046` | Standard conformance suite | todo | AUT-043, AUT-076, AUT-077 | command | ATA-06:03 |
| `AUT-047` | Resolve identity binding scope | dropped | - | manual | - |
| `AUT-048` | Resolve audit format and retention | dropped | - | manual | - |
| `AUT-076` | Requester and controller authentication | todo | AUT-070, AUT-041 | command | ATA-02:05 |
| `AUT-077` | Profile enforcement and startup assertions | todo | AUT-042, AUT-045 | command | ATA-06:03, ATA-06:05 |

## S7: Identity binding and audit coverage

**State:** todo (0/3 done)

**Goal:** Add per-session identity binding and complete audit coverage.

**Exit criteria:** Binding and audit tests green; no identity or secrets in any record.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-049` | Per-session identity binding | todo | AUT-076, AUT-066 | command | ATA-05:03, ATA-05:04, ATA-05:02, INV-ATA-03 |
| `AUT-050` | Audit coverage and checkpoints | todo | AUT-068, AUT-049 | command | ATA-05:05, ATA-06:10 |
| `AUT-051` | Binding and audit tests | todo | AUT-049, AUT-050 | command | - |

## S8: Operations and security review

**State:** todo (0/7 done)

**Goal:** Make the service observable and recoverable, and review its security.

**Exit criteria:** Dashboards, alerts, runbooks and restore drill done; threat model and fuzzing complete.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-078` | Dashboards | todo | AUT-046 | manual | - |
| `AUT-079` | Alerts | todo | AUT-078 | manual | - |
| `AUT-080` | Runbooks | todo | AUT-067, AUT-031 | manual | - |
| `AUT-081` | Backup and restore drill | todo | AUT-075, AUT-080 | manual | - |
| `AUT-082` | Threat model | todo | AUT-046 | manual | - |
| `AUT-083` | Fuzz the parsers and verifiers | todo | AUT-028, AUT-020 | manual | - |
| `AUT-084` | Secret scanning and dependency audit | todo | AUT-010 | manual | - |

## S9: Release and sign-off

**State:** todo (0/8 done)

**Goal:** Run full conformance, regenerate specifications, release and document limits.

**Exit criteria:** Release tagged with conformance report; High documented as unsupported on this stack.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-057` | Full conformance run per profile | todo | AUT-037, AUT-046, AUT-051 | manual | - |
| `AUT-058` | Regenerate specifications | todo | AUT-057 | manual | - |
| `AUT-059` | Release | todo | AUT-058, AUT-086, AUT-087, AUT-088 | manual | - |
| `AUT-060` | Post-release review and plan close | todo | AUT-059 | manual | - |
| `AUT-085` | Versioning | todo | AUT-057 | manual | - |
| `AUT-086` | Staged deployment | todo | AUT-085, AUT-081 | manual | - |
| `AUT-087` | Document High as unsupported on this stack | todo | AUT-077 | files | ATA-06:05, ATA-06:04 |
| `AUT-088` | Conformance report | todo | AUT-057 | manual | - |

## S10: High profile (deferred)

**State:** blocked (0/5 done)

**Goal:** Hardware-backed keys and verifiable evidence. Out of scope on the current stack.

**Exit criteria:** Only started after a scope decision to support High.

| Task | Title | Status | Depends on | Verify | Spec refs |
|---|---|---|---|---|---|
| `AUT-052` | Decide the High key host and evidence source | blocked | - | manual | ATA-06:04 |
| `AUT-053` | Hardware-backed key provider | blocked | AUT-052, AUT-042 | manual | ATA-06:04 |
| `AUT-054` | Publish attestation evidence | blocked | AUT-052 | manual | ATA-06:04, ATA-06:05 |
| `AUT-055` | Enforce mandatory real-time revocation | blocked | AUT-043, AUT-052 | manual | ATA-04:04, ATA-03:05 |
| `AUT-056` | High conformance suite | blocked | AUT-053, AUT-054, AUT-055 | manual | ATA-06:04 |

---

`E-ATA-nn` identifiers in task `findings` refer to the cursory review findings in the amk-authority specification and are informational only.
