# amk-runtime — Runtime Package Authority & Loader Specification

| Field | Value |
|---|---|
| Spec ID | `amk-runtime` |
| Version | 0.1.0-draft |
| Status | Draft — defines the runtime contract, the package format, and the runtime loader and package authority. Items that depend on unresolved review decisions are tracked in `99-open-items.md` as `RTM-O-nn` and referenced inline as "(see RTM-O-nn)". |
| Boundary | Runtime Package Authority & Loader. Defines what a runtime is, how a runtime package is identified, verified, loaded, included, retired and updated, and the contract a runtime must satisfy. |
| Trust | Trusted for package provenance, conformance verification and load integrity. Untrusted for operator authorization and for enforcement. |
| Normative language | MUST / MUST NOT / SHOULD / MAY as in RFC 2119. |

---

## Holistic System Overview

A runtime extends the kernel with capability. The kernel itself (the core OS/10 capabilities scaffolded by the kernel subtree) has no runtime. Everything beyond it, including the bundled system package `amk-core`, arrives as a runtime package.

The amk-runtime component is the single defined authority over **runtime packages**: what a package is, who signed it, whether it conforms to the protocol standards, and when and how its code is brought into execution. It is also the **loader**: the only means by which package code is placed into an execution context for a session.

It is deliberately **not** an authority over authorization or enforcement. The human operator, acting through the console, alone decides which packages are registered and which are included in a session. The controller alone enforces, issues capability tokens and holds state. The Attestation Authority alone issues role attestations. amk-runtime verifies and loads what those parties have already decided; it never decides for them.

Every runtime package, system packages included, must be downloaded and registered by the operator through the console, and its inclusion in a session must be authorized through the console. A console-signed pre-authorization may cover that inclusion where the console spec allows it (see RTM-O-04).

The specification is language-agnostic. Platform-specific loader implementations (for example sandboxed isolates on an edge platform) are described in companion implementation profiles, not here.

Three operational profiles (Basic, Standard, High) govern isolation, verification strength, and audit. Higher profiles subsume all requirements of lower profiles.

This document is an index. Normative requirements live in the module files listed below.

---

## Reference ID Scheme

Every normative statement carries a stable identifier of the form:

**`RTM-<file>:<rule>`**

- `RTM` identifies this specification.
- `<file>` is the module file number (see table below).
- `<rule>` is the rule number inside that file. Each file begins numbering at 1.

Once published, an ID is never reassigned. A withdrawn requirement keeps its ID and is marked `RETIRED`.

---

## Module Index & File Numbers

| File # | File | Scope |
|--------|------|-------|
| 1 | [00-foundations.md](00-foundations.md) | Terms, role, authority limits, trust boundary, invariants |
| 2 | [01-package-format.md](01-package-format.md) | Package structure, manifest, identity, versioning, signatures |
| 3 | [02-contract.md](02-contract.md) | Runtime contract: in-band API, runtime map, channels, UI, errors |
| 4 | [03-lifecycle.md](03-lifecycle.md) | Registration, loading, inclusion, removal, update |
| 5 | [04-loader.md](04-loader.md) | Loader obligations: verification, load order, isolation, limits |
| 6 | [05-package-authority.md](05-package-authority.md) | Provenance, publishers, repositories, update flags, package revocation |
| 7 | [06-system-packages.md](06-system-packages.md) | System packages and `amk-core` |
| 8 | [07-profiles.md](07-profiles.md) | Basic / Standard / High profile requirements |
| 9 | [99-open-items.md](99-open-items.md) | Open questions and defaults needed (non-normative) |

---

## Relationship to Other Specifications

| Specification | Relationship |
|---|---|
| `amk-console` (`CNS`) | The console registers packages, converts a runtime map into a scoped subtree, decides inclusion, and signs every operator decision. amk-runtime never acts without a console-signed decision. |
| `amk-controller` (`CTR`) | The controller owns session state, builds the surface, issues capability tokens and invokes runtimes through the in-band API. The loader runs inside the controller release's trust domain (see RTM-O-01). |
| `amk-registry` / Attestation Authority (`ATA`) | Separate concern. The Authority issues role attestations to session participants; amk-runtime governs packages. Whether runtimes also receive role attestations is open (see RTM-O-16). |
| HTSM companion units | Define the Host AI traversal surface through which runtimes reach the Host AI. |

---

## Operational Profiles (Summary)

| Profile | Target | Verification keys | Isolation | Audit |
|---|---|---|---|---|
| **Basic** | Hobby / educational / prototypes | Software-protected | Soft isolation acceptable | Minimal |
| **Standard** | Production services | Non-exportable, OS keystore or equivalent | Per-runtime process, container or isolate; egress denied by default | Full signed load and lifecycle audit |
| **High** | Enterprise / regulated / high-assurance | HSM, secure element or TEE | Hardware-backed isolation; measured load | Hardware-protected audit; remote attestation of the loader SHOULD be supported |

Detailed obligations are defined in file 8.
