# 00-foundations.md

**File:** `00-foundations.md`
**Scope:** Role, trust boundary, invariants.
**Reference prefix:** `ATA-00`

---

## ATA-00:01 Role

- The Attestation Authority is the sole issuer of role attestations.
- A role attestation binds a session public key to a role for a specific session handle and epoch, within a validity window.
- The Authority never issues long-term identity certificates to the AMK core entities. Identity binding, if supported, stays internal.

## ATA-00:02 Trust Boundary

- Trusted for the statements it signs.
- Untrusted for enforcement: the controller enforces.
- Clients verify signatures against a pinned Authority public key.
- The Authority verifies proof-of-possession before issuance.

## ATA-00:03 Self-Containment

- The Authority only defines the attestation format, issuance and verification rules, revocation semantics, and its own key obligations.
- It does not know or care how clients are implemented.

## ATA-00:04 Invariants

- INV-ATA-01: No attestation without proof-of-possession.
- INV-ATA-02: No attestation for an undeclared role when a role list is supplied.
- INV-ATA-03: Long-term identity is never revealed to session participants.
- INV-ATA-04: Failed verification is opaque denial.
- INV-ATA-05: Revocation is checked when a list is available.
- INV-ATA-06: `valid_to` never exceeds end of epoch plus grace window `G`.

## ATA-00:05 Relationship to Other Components

- amk-controller: consumes attestations; verifies them under ATA-03 before relying on them and decides their effect under its own policy.
- amk-console: no direct interaction.
- Entities: request attestations for assumed roles.

## ATA-00:06 Summary

- The Authority is a self-contained, language-agnostic issuer of role attestations. It is trusted only for what it signs. Enforcement belongs elsewhere.
