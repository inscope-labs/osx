# 04-revocation.md

**File:** `04-revocation.md`
**Scope:** Revocation list / real-time revocation.
**Reference prefix:** `ATA-04`

---

## ATA-04:01 Revocation List

- The Authority SHOULD publish a signed revocation list (RL). [S-09]
- The RL MUST contain:
  - `issuer`: the Authority's identifier.
  - `published_at`: the publication timestamp.
  - `revoked`: a list of revoked attestation identifiers (nonces or unique IDs).
  - `signature`: the Authority's signature over the canonical serialization.
- The RL MUST be signed with the same key as attestations or a dedicated revocation key. [P-09]
- The RL MUST be published at least every `RL_INTERVAL` (default: 60 seconds). [DV-ATA-06]

## ATA-04:02 Revocation Reasons

- An attestation MAY be revoked for:
  - key compromise,
  - role change,
  - session termination,
  - policy violation,
  - or any other reason defined by the Authority or profile.
- The reason MAY be included in the RL as an opaque code. [P-10]

## ATA-04:03 Client Obligations

- Clients MUST check the RL when available. [INV-ATA-05]
- If the client's RL is older than `R_MAX`, it MUST reject the attestation. [S-08]
- If no RL is available:
  - Basic profile: the client MAY proceed.
  - Standard and High profiles: the client MUST reject. [P-08]

## ATA-04:04 Real-Time Revocation Endpoint

- Under the Standard and High profiles, the Authority MAY provide a real-time revocation endpoint. [P-11]
- The endpoint MUST accept a query for a specific attestation and return its revocation status.
- The endpoint MUST be authenticated or rate-limited to prevent abuse.
- Real-time revocation is optional under the Basic profile.

## ATA-04:05 Revocation Propagation

- The Authority MUST propagate revocations as quickly as the profile allows.
- Under Standard and High profiles, revocation SHOULD be effective within `RL_INTERVAL`.
- Under Basic profile, revocation is effective at the next RL publication.

## ATA-04:06 Summary

- The Authority publishes a signed revocation list. Clients check it and reject stale or revoked attestations. Real-time revocation is available under higher profiles. Revocation propagation depends on the profile.

---

## Defaults needed

- Revocation list publication interval (default: 60 seconds) [DV-ATA-06]
- Maximum revocation list age `R_MAX` (default: 300 seconds) [DV-ATA-02]
