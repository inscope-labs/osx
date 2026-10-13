# Envelope Interface

**File number:** 3  
**Overview.**  
This module defines the common validation rules the controller applies to every request envelope and the catalogue of envelope types it exchanges with consoles and other parties. All control-plane interaction with consoles occurs through these envelopes.

---

## Common Validation Rules

- **CTR-3:1** The controller MUST validate every request envelope against canonical serialization, contract version, TTL, clock skew, monotonic sequence number, and a nonce of at least 128 bits.
- **CTR-3:2** The controller MUST reject any envelope whose signature fails verification against a registered console key (or other expected sender key).
- **CTR-3:3** The controller MUST reject any envelope whose timestamp falls outside the allowed skew window.
- **CTR-3:4** The controller MUST reject any envelope whose nonce has been seen before.
- **CTR-3:5** Under Standard and High profiles the controller MUST enforce strict monotonic sequence numbers per sender identity. The Basic profile MAY use weaker sequencing.
- **CTR-3:6** Unknown commands MUST be rejected; existence confers no authority.

## Envelope Catalogue (Core Types)

- **CTR-3:7** The following envelope types are recognized. Additional types may be defined by companion specifications provided they obey the common rules.

| Type | Direction | Purpose |
|---|---|---|
| `SessionStart` / `ExecutionProfile` | Console → Controller | Initiate session and supply work orders, runtimes, context |
| `SurfaceProposal` | Controller → Console | Proposed surface for validation |
| `SurfaceAcceptance` / `SurfaceRejection` | Console → Controller | Result of console validation gates |
| `Handoff` | Controller → Console | Session start credential |
| `RuntimeRegistration` / subtree intake | Console → Controller | Register or supply a runtime subtree |
| `RuntimeInclusionDecision` | Console → Controller | Approve or deny mid-session inclusion |
| `WorkOrder` / `WorkOrderUpdate` | Console → Controller | Create, extend, renew, cancel |
| `PreAuthorization` | Console → Controller | Create, change, or revoke a pre-authorization |
| `Revocation` | Console → Controller | Immediate revocation |
| `StatusQuery` / `StatusResponse` | Console ↔ Controller | State, audit, or risk information |
| `Notification` | Controller → Console | Pending decision or event |
| `CapabilityToken` | Controller → authorized party | Short-lived capability token |
| `ClarificationAnswer` | Console → Controller | Operator answer to a clarification question |
| `ValidatorEvidence` | Console → Controller | Optional session-validator evidence |

- **CTR-3:8** All of the above are ordinary envelopes subject to the common rules. No special out-of-band channels are required for control-plane actions between console and controller.
