# Keys and Authentication

**File number:** 2  
**Overview.**  
This module defines the abstract key hierarchy and the authentication/confirmation requirements. Concrete protection strength is governed by the operational profile (see file 7). The specification remains language- and platform-agnostic.

---

## Key Hierarchy

- **CNS-2:1** The console MUST maintain a console signing key used to sign all envelopes that express operator intent.
- **CNS-2:2** The console MAY maintain a distinct map/surface key used exclusively to sign surface-acceptance envelopes.
- **CNS-2:3** The console MUST maintain pinned verification keys for the controller and any other parties whose envelopes it accepts. These keys are obtained through a trusted bootstrap mechanism.
- **CNS-2:4** Under the Standard and High profiles, all private keys MUST be non-exportable.
- **CNS-2:5** Under the Basic profile, software-protected keys are permitted.

## Confirmation Policy

- **CNS-2:6** Certain envelope types require proof that a human operator has freshly authorized the action. The strength of that proof is profile-dependent (see file 7).
- **CNS-2:7** High-impact actions (session start, pre-authorization creation or change, key changes, revocation of active sessions) MUST require a fresh confirmation under the Standard and High profiles.
- **CNS-2:8** The Basic profile MAY relax confirmation strength for some actions.
- **CNS-2:9** The precise mechanism of “fresh confirmation” is an implementation concern; the resulting envelope MUST carry evidence that the confirmation occurred according to the claimed profile.

## Profile-Dependent Key Protection

(See file 7 for the full normative table. The following are the core statements.)

- **CNS-2:10** Under Standard and High profiles, private keys MUST be protected by an OS keystore or equivalent.
- **CNS-2:11** Under the High profile, private keys MUST be protected by an HSM, secure element, or TEE and MUST NOT exist in ordinary process memory in clear form.
