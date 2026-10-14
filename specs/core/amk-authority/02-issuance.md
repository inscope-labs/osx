# 02-issuance.md

**File:** `02-issuance.md`
**Scope:** Issuance protocol, proof-of-possession, binding rules.
**Reference prefix:** `ATA-02`

---

## ATA-02:01 Issuance Request

- A client requests an attestation by submitting an issuance request envelope.
- The request MUST contain:
  - `session_public_key`: the public key to bind.
  - `role`: the requested role.
  - `session_handle`: the session handle.
  - `epoch`: the epoch number.
  - `proof_of_possession`: a PoP as defined in `ATA-02:03`.
  - `request_nonce`: a unique nonce for the request.
  - `request_timestamp`: the request time.
- The request MAY contain a declared role list. If present, the Authority MUST verify the requested role is in the list. [INV-ATA-02]

## ATA-02:02 Request Validation

- The Authority MUST validate:
  - the request envelope format,
  - the request nonce is fresh (within the freshness window),
  - the request timestamp is within acceptable clock skew,
  - the session handle and epoch are valid.
- On any failure, the Authority MUST reject the request with an opaque error.

## ATA-02:03 Proof-of-Possession

- The requester MUST prove possession of the session private key corresponding to `session_public_key`. [INV-ATA-01]
- The PoP MUST be one of:
  - a signature over a challenge provided by the Authority,
  - a signature over a deterministic value derived from the request fields,
  - or another method defined by the profile.
- The Authority MUST verify the PoP before issuance.
- A failed PoP MUST result in an opaque rejection.

## ATA-02:04 Role Validation

- If the request includes a declared role list, the Authority MUST verify the requested role is present. [INV-ATA-02]
- If the request does not include a role list, the Authority MAY issue the attestation for any role, subject to policy. [P-03]

## ATA-02:05 Identity Authentication (Profile-Dependent)

- Under the Standard and High profiles, the Authority MAY require additional authentication of the long-term identity before issuance. [P-04]
- Under the Basic profile, identity authentication is not required.
- When identity authentication is required, the method MUST be specified in the profile (`ATA-06`).
- Long-term identity MUST NOT be included in the attestation. [INV-ATA-03]

## ATA-02:06 Issuance

- On successful validation, the Authority MUST:
  - generate a unique nonce for the attestation,
  - compute `valid_from` and `valid_to` within the epoch plus grace window,
  - construct the attestation with all required fields,
  - sign the canonical serialization,
  - return the signed attestation to the requester.
- The Authority MUST refuse to issue if any validation fails.

## ATA-02:07 Binding Rules

- An attestation is bound to:
  - the session public key,
  - the role,
  - the session handle,
  - the epoch.
- A change in any of these requires a new attestation.
- The Authority MUST NOT issue an attestation that conflicts with an existing valid attestation for the same session, epoch, and role unless the existing one is revoked. [P-05]

## ATA-02:08 Summary

- Issuance requires proof-of-possession and, under some profiles, identity authentication. The Authority validates the request, verifies the role against any declared list, and issues a signed attestation bound to the session key, role, session handle, and epoch.

---

## Defaults needed

- Nonce freshness window for issuance (default: 60 seconds) [DV-ATA-04]
- PoP challenge length (default: 128-bit nonce) [DV-ATA-05]
