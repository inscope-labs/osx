# Open Items & Migration Notes

**File number:** 8  
**Overview.**  
Non-normative list of remaining open questions and notes on the migration from earlier drafts. This file contains no normative requirements.

---

## Open Questions

- Concrete envelope schemas (field-level definitions, serialization format, mandatory vs optional fields) for each type listed in file 3.
- Exact validation-gate set and versioning rules (companion specification).
- Recovery protocol for Standard and High profiles after key loss or compromise.
- Precise skew windows, nonce lifetime, and sequence-number storage requirements per profile.
- How pre-authorization scope is expressed (single multi-scope envelope vs multiple single-scope envelopes).
- Interaction of Auto Assist policy flags with anomaly levels reported by the controller.
- Bootstrap / pinning mechanism for the controller’s public key and any other external verification keys.

## Migration Notes

- All UI/UX, presentation, operator-load timing, deep-linking, first-run wizards, notification rendering, and platform-specific material has been removed.
- The console is defined solely by its envelope contracts and verification obligations.
- Three operational profiles (Basic / Standard / High) are defined in file 7.
- The specification is language-agnostic.
- Reference IDs now follow the single scheme `CNS-<file>:<rule>`.
