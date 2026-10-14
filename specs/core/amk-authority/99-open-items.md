# 99-open-items.md

**File:** `99-open-items.md`
**Scope:** Open questions.
**Reference prefix:** `ATA-99`

---

## ATA-99:01 Open Items

- **O-ATA-01** Whether the Authority supports multiple simultaneous signing keys for rotation.
- **O-ATA-02** The exact format of the session's declared role list and how it is communicated to the Authority.
- **O-ATA-03** Whether identity binding is per-role or per-session.
- **O-ATA-04** The revocation list distribution mechanism (push, pull, or both).
- **O-ATA-05** How the Authority is enrolled with the controller or router at first use.
- **O-ATA-06** Whether the Authority can delegate issuance to sub-authorities.
- **O-ATA-07** The exact PoP challenge format and whether it is standardized across profiles.
- **O-ATA-08** Whether the Authority publishes metadata (algorithms, profile, public key) at a well-known endpoint.
- **O-ATA-09** The audit format and retention requirements for each profile.
- **O-ATA-10** Whether the Authority supports batch issuance for multiple roles.

## ATA-99:02 Blocking Items

- Items O-ATA-01 through O-ATA-05 are considered blocking for a 1.2.0 release.
- Items O-ATA-06 through O-ATA-10 may be deferred to a later revision.

## ATA-99:03 Summary

- Open items cover key rotation, role list format, identity binding scope, revocation distribution, enrollment, delegation, PoP standardization, metadata publication, audit, and batch issuance.

---

## Defaults

- Grace window `G` (default: end of epoch + 30 seconds) [DV-ATA-01]
- Maximum revocation list age `R_MAX` (default: 300 seconds) [DV-ATA-02]
- Attestation maximum validity window (default: epoch duration + G) [DV-ATA-03]
- Nonce freshness window for issuance (default: 60 seconds) [DV-ATA-04]
- PoP challenge length (default: 128-bit nonce) [DV-ATA-05]
- Revocation list publication interval (default: 60 seconds) [DV-ATA-06]
- Clock skew tolerance (default: 30 seconds) [DV-ATA-07]

---

## Dependent files

Drafted for 1.2 together with this file: `session-router` (including its `router-factory` endpoint). Not yet updated: `amk-test-matrix`, `ssot-index`. Discontinued: `amk-contracts`, `os10-lifecycle`, `amk-defaults-manifest`.
