# amk-authority: directory and file structure (reference guide)

**Location in the repository:** `impl/core/amk-authority/STRUCTURE.md`
**Derived from:** the implementation plan in `plan/` (88 tasks, 11 stages). The `deliverables` paths in the task files are the source of truth. This guide adds only what the plan's verification commands imply, marked *(implied)*.
**Specification:** `specs/core/amk-authority/` (rule IDs `ATA-nn:nn`).

---

## 1. Placement rules

1. Everything for this component lives under `impl/core/amk-authority/`. The repository root belongs to the Android app and its `AGENTS.md`; nothing here depends on it.
2. `impl/core/amk-authority/AGENTS.md` is the only instruction file for this directory.
3. Code lives in four packages. Tests live inside the package they test, in `test/`, never in a shared top-level test folder.
4. No secrets in the repository. Bindings and secret names are documented; values are not stored.
5. Workflows live in the repository's `.github/workflows/`, are separate files with an `amk-authority-` prefix, and run only on `workflow_dispatch`.
6. Only the plan tool writes `PLAN.md`. Do not edit it by hand.
7. Work arrives as submissions under `submissions/<submission_id>/`; the supervisor moves the deliverables to their final paths and keeps `SUBMISSION.json`, each `REPORT.md` and `EXIT-REPORT.md` as the record. They hold no secrets.
8. One task, one branch, one pull request. A task's deliverables are the files it creates or changes.

---

## 2. Directory tree

Annotations show the task that creates each item. Items without a task ID are *(implied)*.

```
impl/core/amk-authority/
├── AGENTS.md                              [AUT-007]  local instructions, overrides repository root
├── README.md                              [AUT-087]  includes "High not supported on this stack"
├── STRUCTURE.md                                      this guide
├── PLAN.md                                           generated from plan/, do not edit
├── submissions/                                      staging and archive, one folder per submission
│   └── <submission_id>/                              SUBMISSION.json, EXIT-REPORT.md, <task_dir>/REPORT.md (files/ is staged, then moved by the supervisor)
├── package.json                           [AUT-008]  workspace root
├── pnpm-workspace.yaml                    [AUT-008]
├── pnpm-lock.yaml                                    (implied)
├── tsconfig.base.json                     [AUT-063]
├── eslint.config.js                                  (implied) shared lint rules
├── .gitignore                                        (implied) .dev.vars, .wrangler/, node_modules/
│
├── plan/                                             the implementation plan
│   ├── stages.json                                   stage boundaries
│   ├── plan_tool.py                                  check, status, render, verify, set, registry, coverage
│   ├── test_plan_tool.py                                 self-tests for the plan tool
│   ├── coverage_exempt.json                              specification items intentionally not cited, with reasons
│   └── tasks/
│       └── AUT-001.json … AUT-090.json               one file per atomic task
│
├── docs/
│   ├── trust-anchor.md                        [AUT-090]  root key provisioning and pinning
│   ├── runbooks.md                            [AUT-080]  rotation, emergency revocation, restore
│   ├── threat-model.md                        [AUT-082]
│   └── versioning.md                          [AUT-085]
│
└── packages/
    │
    ├── shared-crypto/                                used by authority and client-sdk
    │   ├── package.json                   [AUT-008]
    │   ├── src/
    │   │   ├── attestation.ts             [AUT-012]  type and field validation
    │   │   ├── jcs.ts                     [AUT-013]  RFC 8785 canonicalizer
    │   │   ├── ed25519.ts                 [AUT-014]  sign, verify, kid (RFC 7638)
    │   │   ├── validity.ts                [AUT-015]  window and skew rules
    │   │   ├── nonce.ts                   [AUT-016]  attestation nonce generation
    │   │   ├── signer.ts                  [AUT-025]  Signer interface, SoftwareSigner
    │   │   └── proxy-signer.ts            [AUT-042]  Standard key-host signer
    │   └── test/                                     (implied) one *.test.ts per source file
    │
    ├── authority/                                    the Worker and Durable Object
    │   ├── package.json                   [AUT-008]
    │   ├── wrangler.jsonc                 [AUT-064]  D1, R2, KV, Durable Object bindings (names only)
    │   ├── migrations/
    │   │   ├── 0001_init.sql              [AUT-065]  tables
    │   │   └── 0002_constraints.sql       [AUT-066]  unique constraints and indexes
    │   ├── src/
    │   │   ├── index.ts                   [AUT-009]  Worker entry and routing
    │   │   ├── authority-do.ts            [AUT-009]  Durable Object (serialization only)
    │   │   ├── errors.ts                  [AUT-069]  opaque error mapping, correlation ID
    │   │   ├── audit.ts                   [AUT-068]  hash-chained audit writer, redaction
    │   │   ├── audit-checkpoint.ts        [AUT-050]  signed checkpoints, R2 archival
    │   │   ├── metadata.ts                [AUT-045]  signed metadata document
    │   │   ├── rotation.ts                [AUT-067]  key statuses, publication, rollback
    │   │   ├── session-record.ts          [AUT-024]  controller session record intake
    │   │   ├── auth.ts                    [AUT-076]  mTLS and JWT client authentication
    │   │   ├── retention.ts               [AUT-075]  cleanup and archival jobs
    │   │   ├── issuance/
    │   │   │   ├── request.ts             [AUT-019]  request schema and validation
    │   │   │   ├── pop.ts                 [AUT-020]  deterministic proof of possession
    │   │   │   ├── roles.ts               [AUT-021]  role vs session record
    │   │   │   ├── replay.ts              [AUT-022]  request nonce protection
    │   │   │   ├── issue.ts               [AUT-023]  assembly, signing, binding rules
    │   │   │   └── endpoint.ts            [AUT-070]  POST /v1/attestations
    │   │   ├── revocation/
    │   │   │   ├── list.ts                [AUT-029]  list structure and signing
    │   │   │   ├── revoke.ts              [AUT-030]  POST /v1/revocations
    │   │   │   ├── publish.ts             [AUT-031]  alarm-driven publication
    │   │   │   └── status.ts              [AUT-043]  real-time signed status
    │   │   ├── identity/
    │   │   │   └── binding.ts             [AUT-049]  per-session identity binding
    │   │   └── profiles/
    │   │       ├── basic.ts               [AUT-039]  Basic profile
    │   │       └── standard.ts            [AUT-077]  Standard enforcement, startup assertions
    │   └── test/
    │       ├── issuance.e2e.test.ts       [AUT-026]
    │       └── …                                     (implied) one *.test.ts per source file
    │
    ├── client-sdk/                                   verification, reused by the controller
    │   ├── package.json                   [AUT-008]
    │   ├── README.md                      [AUT-074]  controller integration contract
    │   ├── src/
    │   │   ├── metadata.ts                [AUT-071]  metadata resolver and cache
    │   │   ├── verify.ts                  [AUT-027]  verifier pipeline, opaque verdict
    │   │   ├── presentation.ts            [AUT-028]  presentation proof, session context
    │   │   ├── nonce-store.ts             [AUT-072]  verifier nonce store interface
    │   │   ├── revocation.ts              [AUT-032]  list handling, fail closed
    │   │   └── profile.ts                 [AUT-073]  peer profile rule
    │   └── test/
    │       ├── verify.negative.test.ts    [AUT-034]
    │       ├── no-verdict-cache.test.ts   [AUT-089]
    │       └── …                                     (implied) one *.test.ts per source file
    │
    └── conformance/                                  proves the spec, not just the code
        ├── package.json                   [AUT-008]
        ├── vectors/
        │   └── v1/
        │       ├── README.md              [AUT-017]  vector schema, for other languages
        │       └── *.json                            (implied) valid, malformed, expired, tampered …
        ├── harness/
        │   └── README.md                  [AUT-035]  controller stand-in, public endpoints and SDK only
        └── test/
            ├── vectors.test.ts            [AUT-018]  runs every vector
            ├── fuzz.test.ts               [AUT-083]
            ├── integration.e2e.test.ts    [AUT-036]
            ├── basic.test.ts              [AUT-037]
            ├── standard.test.ts           [AUT-046]
            └── binding-audit.test.ts      [AUT-051]
```

Files outside this directory that the plan creates or changes:

```
.github/workflows/
├── amk-authority-ci.yml                   [AUT-010]  typecheck and test, manual only
├── amk-authority-deploy.yml               [AUT-010]  deploy, manual only
└── amk-authority-plan.yml                            plan check, status, verify, render (exists)

specs/core/amk-authority/                  [AUT-061, AUT-062]  specification (9 files + generated spec-full)
```

---

## 3. Package responsibilities and dependency direction

| Package | Responsibility | May depend on |
|---|---|---|
| `shared-crypto` | Canonical serialization, Ed25519, key identifiers, signer interface, validity and nonce rules. No network, no storage. | nothing in this directory |
| `authority` | The Worker: metadata, issuance, session records, revocation, audit, retention, profile enforcement. | `shared-crypto` |
| `client-sdk` | Verification once, reused by the controller and any client. No knowledge of the Authority's storage. | `shared-crypto` |
| `conformance` | Vectors, controller stand-in, conformance suites. Reaches the Authority only through its public HTTP surface and the SDK. | `shared-crypto`, `client-sdk` |

```
shared-crypto ◄── authority
      ▲
      └────────── client-sdk ◄── conformance
```

Rules that follow from the plan:
- `authority` never imports `client-sdk`, and `client-sdk` never imports `authority`.
- One canonicalizer, one signer interface, one verifier. No endpoint reimplements them.
- The Durable Object serializes revocation list builds, metadata publication and the audit head. Atomic guards for races are D1 unique constraints, not application checks.

---

## 4. Specification to code map

| Spec rule | Implemented in |
|---|---|
| `ATA-01:01` | `shared-crypto/src/attestation.ts` |
| `ATA-01:02` | `shared-crypto/src/jcs.ts` |
| `ATA-01:03` | `shared-crypto/src/validity.ts` |
| `ATA-01:04` | `shared-crypto/src/nonce.ts` |
| `ATA-01:05` | `shared-crypto/src/ed25519.ts`, `signer.ts` |
| `ATA-01:07` | `authority/src/metadata.ts`, `rotation.ts`; `client-sdk/src/metadata.ts` |
| `ATA-02:01`, `ATA-02:02` | `authority/src/issuance/request.ts`, `session-record.ts`, `issuance/replay.ts` |
| `ATA-02:03` | `authority/src/issuance/pop.ts` |
| `ATA-02:04` | `authority/src/issuance/roles.ts` |
| `ATA-02:05` | `authority/src/auth.ts` |
| `ATA-02:06`, `ATA-02:07` | `authority/src/issuance/issue.ts`, `endpoint.ts` |
| `ATA-03:01` to `ATA-03:03` | `client-sdk/src/verify.ts` |
| `ATA-03:04` | `client-sdk/src/presentation.ts`, `nonce-store.ts` |
| `ATA-03:05`, `ATA-04:03` | `client-sdk/src/revocation.ts` |
| `ATA-03:07` | `client-sdk/README.md` (contract), `client-sdk/src/verify.ts` (opaque verdict) |
| `ATA-04:01` | `authority/src/revocation/list.ts`, `publish.ts` |
| `ATA-04:02` | `authority/src/revocation/revoke.ts` |
| `ATA-04:04` | `authority/src/revocation/status.ts` |
| `ATA-05:03`, `ATA-05:04` | `authority/src/identity/binding.ts` |
| `ATA-06:02` to `ATA-06:04` | `authority/src/profiles/basic.ts`, `standard.ts` (High deferred) |
| `ATA-06:05`, `ATA-06:07` | `client-sdk/src/profile.ts`, `metadata.ts` |
| `ATA-06:09` | `conformance/test/*.test.ts` |
| `ATA-06:10`, `ATA-05:05` | `authority/src/audit.ts`, `audit-checkpoint.ts`, `retention.ts` |
| `INV-ATA-04` | `authority/src/errors.ts`, `client-sdk/src/verify.ts` |

---

## 5. Conventions

- **Tests:** `packages/<pkg>/test/<source-name>.test.ts`, one per source file. The plan's `pnpm --filter @amk-authority/<pkg> test -- <name>` commands match on the name.
- **Package names:** `@amk-authority/shared-crypto`, `@amk-authority/authority`, `@amk-authority/client-sdk`, `@amk-authority/conformance`.
- **Migrations:** numbered `NNNN_description.sql`, idempotent, never edited after merge; a later change is a new file.
- **Vectors:** JSON, language-neutral, versioned by folder (`v1/`). Other implementations, such as the Kotlin console, consume the same files.
- **Source layout:** a folder per spec area (`issuance/`, `revocation/`, `identity/`, `profiles/`); single-purpose files at `src/` root.
- **Errors and logs:** one denial response shape with a correlation ID. Internal reasons go to logs only, redacted at the point of writing.
- **IDs:** task IDs `AUT-nnn` are never reused. A removed task is marked `dropped`, not renumbered.

---

## 6. Keeping this guide current

When a task adds, moves or removes a deliverable, update its `deliverables` in `plan/tasks/AUT-nnn.json` first, run `python3 impl/core/amk-authority/plan/plan_tool.py check`, then update this guide in the same pull request. If the two disagree, the plan wins.
