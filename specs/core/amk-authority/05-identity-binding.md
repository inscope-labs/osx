# 05-identity-binding.md

**File:** `05-identity-binding.md`
**Scope:** Optional long-term identity binding (profile-dependent).
**Reference prefix:** `ATA-05`

---

## ATA-05:01 Purpose

- Identity binding allows the Authority to associate a session public key with a long-term identity for policy or audit purposes.
- Identity binding is optional and profile-dependent.
- Long-term identity MUST NOT be revealed to session participants. [INV-ATA-03]

## ATA-05:02 When Identity Binding Applies

- Identity binding MAY be required under the Standard and High profiles.
- Identity binding is not required under the Basic profile.
- The Authority MUST document whether identity binding is enabled and what identity providers are supported.

## ATA-05:03 Binding Process

- When identity binding is required, the Authority MUST authenticate the long-term identity before issuance.
- The authentication method MUST be specified in the profile (`ATA-06`).
- On successful authentication, the Authority MAY store the binding internally.
- The binding MUST NOT be included in the attestation or revealed to any session participant. [INV-ATA-03]

## ATA-05:04 Binding Scope

- Identity binding MAY be per-role or per-session. [O-ATA-03]
- The scope MUST be documented and consistent.
- A change in binding scope requires a new attestation.

## ATA-05:05 Audit

- The Authority MUST audit identity binding events.
- The audit MUST record the binding, the identity provider, and the authentication method.
- The audit MUST NOT reveal long-term identity to unauthorized parties.

## ATA-05:06 Summary

- Identity binding is optional and profile-dependent. When enabled, the Authority authenticates long-term identity before issuance but never reveals it to session participants. Binding scope is documented.

---

## Open items

- **O-ATA-03** Whether identity binding is per-role or per-session.
