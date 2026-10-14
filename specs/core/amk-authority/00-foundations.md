# 00-foundations.md

**File:** `00-foundations.md`
**Scope:** Role, trust boundary, invariants.
**Reference prefix:** `ATA-00`

---

## ATA-00:01 Role

- The Attestation Authority is the sole issuer of role attestations. [S-01]
- A role attestation binds a session public key to a role for a specific session handle and epoch, within a validity window.
- The Authority never issues long-term identity certificates to the router. Identity binding, if supported, stays internal. [S-02]

## ATA-00:02 Trust Boundary

- Trusted for the statements it signs. [S-05]
- Untrusted for enforcement: the router and controller enforce.
- Clients verify signatures against a pinned Authority public key. [S-06]
- The Authority verifies proof-of-possession before issuance.

## ATA-00:03 Self-Containment

- The Authority only defines the attestation format, issuance and verification rules, revocation semantics, and its own key obligations.
- It does not know or care how clients are implemented. [S-03]

## ATA-00:04 Invariants

- INV-ATA-01: No attestation without proof-of-possession.
- INV-ATA-02: No attestation for an undeclared role when a role list is supplied.
- INV-ATA-03: Long-term identity is never revealed to session participants.
- INV-ATA-04: Failed verification is opaque denial.
- INV-ATA-05: Revocation is checked when a list is available.
- INV-ATA-06: `valid_to` never exceeds end of epoch plus grace window `G`.

## ATA-00:05 Relationship to Other Components

- Session Router: verifies attestations; never sees long-term identity.
- Router Factory: may require attestations for Binding Data.
- amk-controller: may consume attestations for Host AI capability or HIL.
- amk-console: no direct interaction.
- Entities: request attestations for assumed roles.

## ATA-00:06 Summary

- The Authority is a self-contained, language-agnostic issuer of role attestations. It is trusted only for what it signs. Enforcement belongs elsewhere.
