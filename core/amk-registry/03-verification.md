# 03-verification.md

**File:** `03-verification.md`
**Scope:** Verification obligations for clients.
**Reference prefix:** `ATA-03`

---

## ATA-03:01 Verification Obligations

- A client receiving a role attestation MUST verify:
  - the signature against the pinned Authority public key,
  - the validity window (`valid_from` <= now <= `valid_to`),
  - the nonce freshness where applicable,
  - the revocation status (`ATA-04`),
  - the session handle and epoch match the current session context.
- A client MUST reject an attestation that fails any check.

## ATA-03:02 Signature Verification

- The client MUST use the Authority's pinned public key to verify the signature. [S-06]
- The client MUST verify the signature over the canonical serialization of the attestation fields.
- A signature verification failure MUST result in opaque denial. [INV-ATA-04]

## ATA-03:03 Validity Window Verification

- The client MUST check that the current time is within `valid_from` and `valid_to`.
- The client MUST reject an attestation outside its validity window.
- The client SHOULD allow a small clock skew tolerance as defined in the defaults. [P-06]

## ATA-03:04 Nonce Freshness

- Where the client tracks nonces, it MUST reject an attestation with a replayed nonce. [P-07]
- The nonce freshness window is defined in the defaults.
- The client MAY skip nonce tracking if the attestation is single-use or short-lived.

## ATA-03:05 Revocation Status

- The client MUST check the revocation list when available. [INV-ATA-05]
- If the client's revocation list is older than `R_MAX`, it MUST reject the attestation. [S-08]
- If no revocation list is available, the client MAY proceed under the Basic profile, but MUST reject under Standard and High profiles. [P-08]

## ATA-03:06 Session Context Verification

- The client MUST verify that the `session_handle` and `epoch` in the attestation match the current session context.
- A mismatch MUST result in opaque denial. [INV-ATA-04]

## ATA-03:07 Router and Factory Behavior

- The router and factory treat a failed verification as opaque denial / registration failure. [INV-ATA-04]
- The router and factory MUST NOT distinguish failure causes to unauthenticated parties.
- The router and factory MUST log verification failures for audit.

## ATA-03:08 Summary

- Clients verify signature, validity window, nonce freshness, revocation status, and session context. A failed verification is opaque denial. Router and factory behavior follows the same rules.

---

## Defaults needed

- Maximum revocation list age `R_MAX` (default: 300 seconds) [DV-ATA-02]
- Clock skew tolerance (default: 30 seconds) [DV-ATA-07]
