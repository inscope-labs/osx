# Session Lifecycle

**File number:** 4  
**Overview.**  
This module defines the envelope exchanges that constitute session start, handoff handling, and mid-session runtime inclusion or removal. All steps are expressed solely as envelope types and verification obligations.

---

## Session Start

- **CNS-4:1** The console initiates a session by emitting a `SessionStart` envelope (and any required follow-on `ExecutionProfile` or work-order envelopes).
- **CNS-4:2** The controller is expected to respond with a session handle and a `SurfaceProposal` envelope.
- **CNS-4:3** The console MUST apply validation gates (file 6) before emitting a `SurfaceAcceptance` envelope.
- **CNS-4:4** On validation failure the console MUST emit a `SurfaceRejection` envelope.
- **CNS-4:5** After acceptance the controller is expected to return a `Handoff` envelope.
- **CNS-4:6** A covering pre-authorization MAY allow the console to emit `SurfaceAcceptance` without additional operator confirmation, subject to profile rules (file 7).
- **CNS-4:7** The operator MAY have multiple concurrent sessions; each is identified by its session handle.

## Handoff

- **CNS-4:8** The `Handoff` is an expiring credential. The console treats it as opaque data.
- **CNS-4:9** Under the Standard and High profiles the console MUST NOT log the handoff value in clear text.
- **CNS-4:10** The console MAY emit a `HandoffDelivery` or `HandoffReceipt` envelope according to the delivery path.

## Mid-Session Runtime Inclusion / Removal

- **CNS-4:11** An external party MAY emit a `RuntimeInclusionRequest` envelope.
- **CNS-4:12** The console responds with a `RuntimeInclusionDecision` envelope (accept or reject).
- **CNS-4:13** Under a covering pre-authorization the decision MAY be emitted automatically according to profile rules.
- **CNS-4:14** Inclusion does not require a new handoff; the existing session handle continues.
- **CNS-4:15** The console MAY emit a `Revocation` envelope targeting a runtime within a session.
