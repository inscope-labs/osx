# 06-profiles.md

**File:** `06-profiles.md`
**Scope:** Basic / Standard / High requirements.
**Reference prefix:** `ATA-06`

---

## ATA-06:01 Profile Overview

- The Authority supports three operational profiles:
  - **Basic:** software keys allowed, simplified revocation, suitable for any language.
  - **Standard:** non-exportable keys (OS keystore or equivalent), full revocation list, process isolation.
  - **High:** HSM/TEE-protected signing keys, measured boot / remote attestation of the Authority itself, hardware-protected audit of issuance. [S-10]
- The profile in use MUST be documented and discoverable by clients.

## ATA-06:02 Basic Profile

- Signing keys MAY be software-protected.
- Revocation list is optional; real-time revocation is not required.
- Identity binding is not required.
- Process isolation is not required.
- Suitable for development, testing, or low-risk deployments.

## ATA-06:03 Standard Profile

- Signing keys MUST be non-exportable and stored in an OS keystore or equivalent. [S-11]
- Revocation list is required.
- Real-time revocation endpoint is permitted.
- Identity binding MAY be required.
- Process isolation MUST be enforced.
- Suitable for production deployments with moderate security requirements.

## ATA-06:04 High Profile

- Signing keys MUST be HSM- or TEE-protected. [S-12]
- Measured boot and remote attestation of the Authority itself SHOULD be supported.
- Hardware-protected audit of issuance MUST be enabled.
- Revocation list is required.
- Real-time revocation endpoint is required.
- Identity binding SHOULD be required.
- Process isolation MUST be enforced.
- Suitable for high-security deployments.

## ATA-06:05 Profile Selection

- The profile MUST be selected at deployment time.
- The profile MUST NOT be changed without re-provisioning.
- Clients MUST verify the profile matches their requirements before trusting attestations. [P-14]

## ATA-06:06 Profile Summary

| Requirement | Basic | Standard | High |
|-------------|-------|----------|------|
| Signing key protection | Software | OS keystore | HSM/TEE |
| Revocation list | Optional | Required | Required |
| Real-time revocation | No | Permitted | Required |
| Identity binding | No | Optional | Should |
| Process isolation | No | Required | Required |
| Measured boot / remote attestation | No | No | Should |
| Hardware audit | No | No | Required |

## ATA-06:08 Summary

- Three profiles define the security requirements for the Authority. Basic is suitable for any language; Standard requires non-exportable keys and full revocation; High requires HSM/TEE protection, with measured boot and remote attestation recommended. The profile is selected at deployment and discoverable by clients.

## ATA-06:07 Peer Profile Rule

- A party MUST NOT accept a peer that declares a lower profile than its own unless the operator or administrator has explicitly allowed it, and that allowance MUST be audited.
