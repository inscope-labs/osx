# amk-prompt — Host AI Kernel Prompt Specification

| Field | Value |
|---|---|
| Spec ID | `amk-prompt` |
| Version | 2.0.0-draft |
| Status | Draft — advisory, stateless, signed prompt contract. |
| Boundary | Host AI kernel prompt and interface contract. |
| Trust | Advisory only. Never authoritative for enforcement or authorization. |
| Normative language | MUST / MUST NOT / SHOULD / MAY as in RFC 2119. |

---

## Holistic System Overview

The amk-prompt specification defines the **kernel prompt** that is loaded into a Host AI at session start. It is an advisory, stateless, versioned, and detached-signed contract. It establishes kernel identity, the command boundary, and the instruction hierarchy. It points to the controller as the sole enforcement point and the console as the sole human authorization plane.

The prompt contains no secrets, no mutable state, no authoritative policy, and no authoritative authorization. Effects are gated by Capability Tokens and controller decisions, not by this prompt. Prompt compliance is defense-in-depth only.

The deployable artifact is generated from this specification as a templated prompt. The template and this specification MUST NOT diverge.

This document is the single source of truth. Normative requirements live in the module files listed below.

---

## Reference ID Scheme

Every normative statement carries a stable identifier of the form:

**`PMT-<file>:<rule>`**

- `PMT` identifies this specification (amk-prompt).
- `<file>` is the module file number.
- `<rule>` is the rule number inside that file. Each file begins numbering at 1.

---

## Module Index & File Numbers

| File # | File | Scope |
|--------|------|-------|
| 1 | [00-foundations.md](00-foundations.md) | Role, trust, assurance levels, statelessness, invariants |
| 2 | [01-identity-bootstrap.md](01-identity-bootstrap.md) | Kernel identity exposure and bootstrap interaction |
| 3 | [02-instruction-boundary.md](02-instruction-boundary.md) | Instruction classes, precedence, and boundary |
| 4 | [03-command-set.md](03-command-set.md) | Command set declaration and non-responsibilities |
| 5 | [04-authority-capability.md](04-authority-capability.md) | Authority rules, capability protection, non-authority declaration |
| 6 | [05-behavior-failure.md](05-behavior-failure.md) | Injection resistance, state handling, failure, inert behavior |
| 7 | [06-htsm-conduct.md](06-htsm-conduct.md) | HTSM traversal, work orders, runtime inclusion, optional validation |
| 8 | [99-open-items.md](99-open-items.md) | Open questions (non-normative) |
