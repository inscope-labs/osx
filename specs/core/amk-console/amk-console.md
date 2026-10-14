# amk-console — User Control Plane Specification

| Field | Value |
|---|---|
| Spec ID | `amk-console` |
| Version | 2.1.0-draft |
| Suite compatibility | `OS10-AMK 2.x` (suite version defined in `amk-ssot.md`) |
| Status | Draft — self-contained, language-agnostic, envelope-only contract with three operational profiles. |
| Boundary | User Control Plane. Defines only the console’s interaction surface. |
| Trust | Trusted for authenticated operator intent. Untrusted for enforcement. |
| Normative language | MUST / MUST NOT / SHOULD / MAY as in RFC 2119. |

---

## Holistic System Overview

The amk-console is the sole seat of human operator authority. It is a self-contained component that communicates with all external entities exclusively through signed envelopes. It has no knowledge of the internal behaviour of the controller, Host AI, runtimes, gateways, or any other counterpart; it only knows the envelope schemas, the cryptographic verification rules it must apply, and the sequencing obligations defined in this specification.

The console is trusted to capture and transmit authenticated operator intent. It is untrusted for enforcement: every action is submitted as a signed envelope that the receiving party alone enforces. Mutual verification is mandatory.

All external interactions are envelope-based. Local presentation, user-interface design, notification rendering, first-run experience, and operator-load timing are outside the scope of this specification and belong in an implementation guide.

The specification is language-agnostic. Implementations may be written in any language that can satisfy the cryptographic and sequencing requirements of the claimed operational profile.

Three operational profiles—Basic, Standard, and High—govern privacy, security, and implementation complexity. Higher profiles subsume all requirements of lower profiles.

Normative requirements live in the module files listed below.

---

## Reference ID Scheme

Every normative statement carries a stable identifier of the form:

**`CNS-<file>:<rule>`**

- `CNS` identifies this specification.
- `<file>` is the module file number (see table below).
- `<rule>` is the rule number inside that file. Each file begins numbering at 1.

Once published, an ID is never reassigned. A withdrawn requirement keeps its ID and is marked `RETIRED`. File numbers are stable identifiers and need not follow file-name order.

---

## Module Index & File Numbers

| File # | File | Scope |
|--------|------|-------|
| 1 | [00-foundations.md](00-foundations.md) | Terms, role, trust boundary, invariants |
| 2 | [01-keys-authentication.md](01-keys-authentication.md) | Key hierarchy and authentication requirements |
| 3 | [02-envelope-interface.md](02-envelope-interface.md) | Envelope catalogue and common rules |
| 4 | [03-session-lifecycle.md](03-session-lifecycle.md) | Session start, handoff, mid-session changes |
| 5 | [04-runtimes-work-preauth.md](04-runtimes-work-preauth.md) | Runtime registration, work orders, pre-authorization, Auto Assist |
| 6 | [05-validation-revocation.md](05-validation-revocation.md) | Validation gates, state rules, revocation, recovery |
| 7 | [06-profiles.md](06-profiles.md) | Basic / Standard / High profile requirements |
| 8 | [99-open-items.md](99-open-items.md) | Open questions (non-normative) |

---

## Operational Profiles (Summary)

| Profile | Target | Key Protection | Isolation | Confirmation | Audit |
|---|---|---|---|---|---|
| **Basic** | Hobby / educational / rapid prototypes | Software keys permitted | Soft isolation acceptable | Risk-based; some high-impact actions may use weaker confirmation | Minimal |
| **Standard** | Production services | Non-exportable keys; OS keystore or equivalent | Process / container isolation | Fresh confirmation for all high-impact actions | Full envelope audit trail |
| **High** | Enterprise / regulated / high-assurance | Hardware-backed or HSM; keys never in ordinary process memory | Strong isolation (TEE or equivalent) | Strict fresh confirmation + measured environment | Hardware-protected audit + remote attestation support |

Detailed obligations are defined in file 7.
