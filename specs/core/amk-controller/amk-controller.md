# amk-controller — Kernel Enforcement Point Specification

| Field | Value |
|---|---|
| Spec ID | `amk-controller` |
| Version | 2.0.0-draft |
| Status | Draft — self-contained, language- and platform-agnostic, with three operational profiles. |
| Boundary | Kernel Enforcement Point. Sole trusted enforcement, token-issuance, and state authority. |
| Trust | Sole trusted enforcement point. |
| Normative language | MUST / MUST NOT / SHOULD / MAY as in RFC 2119. |

---

## Holistic System Overview

The amk-controller is the sole authoritative enforcement point, token-issuance engine, and state authority. It holds the complete picture of session and epoch state. Principals are consoles identified by cryptographic keys; the controller has no notion of a human operator.

All control-plane interaction with consoles occurs through signed envelopes. The controller verifies console signatures and envelope validity; it does not rely on the internal implementation of any console. Interactions with Host AI channels (HTSM and any additional channels) and with runtimes are defined by protocol contracts. The controller treats external parties as black boxes beyond the verification and protocol obligations stated in this specification.

The specification is language- and platform-agnostic. An implementation may be written in any language and run on any platform that can satisfy the cryptographic, isolation, durability, and sequencing requirements of the claimed operational profile. Higher profiles (especially High) may exclude languages or platforms that cannot provide the required key protection, isolation, or audit strength.

Three operational profiles—Basic, Standard, and High—govern privacy, security, and implementation complexity. Higher profiles subsume all requirements of lower profiles.

Local presentation, user-interface design, and operator-facing summaries are outside the scope of this specification and belong in an implementation guide.

This document is the single source of truth. Normative requirements live in the module files listed below.

---

## Reference ID Scheme

Every normative statement carries a stable identifier of the form:

**`CTR-<file>:<rule>`**

- `CTR` identifies this specification.
- `<file>` is the module file number (see table below).
- `<rule>` is the rule number inside that file. Each file begins numbering at 1.

Rule numbers may be reassigned or may point to an updated rule in a later revision. Once published, an ID is never reused for a different requirement.

---

## Module Index & File Numbers

| File # | File | Scope |
|--------|------|-------|
| 1 | [00-foundations.md](00-foundations.md) | Role, trust, authority, invariants |
| 2 | [01-bootstrap-state.md](01-bootstrap-state.md) | Bootstrap, state model, lifecycle |
| 3 | [02-envelope-interface.md](02-envelope-interface.md) | Envelope validation and catalogue |
| 4 | [03-console-identity.md](03-console-identity.md) | Console identity and administration |
| 5 | [04-surface-runtime.md](04-surface-runtime.md) | Surface construction, runtime intake, HTSM |
| 6 | [05-work-preauth-capability.md](05-work-preauth-capability.md) | Work orders, pre-authorization, capability, anomaly |
| 7 | [06-tokens-audit-revocation.md](06-tokens-audit-revocation.md) | Capability tokens, audit, revocation, recovery |
| 8 | [07-profiles.md](07-profiles.md) | Basic / Standard / High profile requirements |
| 9 | [99-open-items.md](99-open-items.md) | Open questions (non-normative) |

---

## Operational Profiles (Summary)

| Profile | Target | Key Protection | Isolation | Durability & Audit | Language / Platform |
|---|---|---|---|---|---|
| **Basic** | Hobby / educational / prototypes | Software keys permitted | Soft isolation acceptable | Minimal durable state | Any language/platform that meets basic crypto |
| **Standard** | Production services | Non-exportable keys; OS keystore or equivalent | Process / container isolation | Full durable state + signed audit chain | Languages/platforms with mature isolation & crypto |
| **High** | Enterprise / regulated / high-assurance | HSM / TEE; keys never in clear process memory | Strong isolation (TEE or equivalent) | Hardware-protected audit + remote attestation | Only languages/platforms that can meet TEE / HSM / measured-boot requirements |

Detailed obligations are defined in file 8. Higher profiles may exclude languages or platforms that cannot satisfy the required isolation, key protection, or audit strength.
