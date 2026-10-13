# Open Items & Migration Notes

**File number:** 9  
**Overview.**  
Non-normative list of remaining open questions and notes on the migration from the previous draft. This file contains no normative requirements.

---

## Open Questions

- Concrete envelope schemas (field-level definitions, serialization format) for each type listed in file 3.
- Exact durable-state requirements that must survive a restart (profile-dependent).
- Console enrollment / key-registration procedure with the controller.
- Provenance binding between a provider-signed runtime map and the console-derived subtree.
- Definition of the capability tests and the kernel-level matrix.
- Channels beyond HTSM and how the kernel operating level gates them.
- Recovery protocol for Standard and High profiles after key loss or compromise.
- Precise skew windows, nonce lifetime, and sequence-number storage requirements per profile.
- Work-order cost unit and concurrent work-order limits.
- Gateway / export definitions.

## Migration Notes

- All references to Cloudflare (or any specific platform or vendor) have been removed.
- The specification is language- and platform-agnostic. Higher profiles may exclude languages or platforms that cannot meet the required isolation, key protection, or audit strength.
- All UI/UX and presentation material has been removed.
- Control-plane interaction with consoles is expressed solely through envelopes.
- Three operational profiles (Basic / Standard / High) are defined in file 8.
- Reference IDs follow the single scheme `CTR-<file>:<rule>`.
