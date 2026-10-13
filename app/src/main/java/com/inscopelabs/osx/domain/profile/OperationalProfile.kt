package com.inscopelabs.osx.domain.profile

/**
 * CNS-7: Operational Profiles.
 * Controls security posture, cryptographic primitives, and execution safeguards.
 */
enum class OperationalProfile(
    val title: String,
    val securityLevel: Int,
    val description: String
) {
    BASIC(
        title = "BASIC",
        securityLevel = 1,
        description = "Software-backed cryptographic providers, simulated transports, development mode."
    ),
    STANDARD(
        title = "STANDARD",
        securityLevel = 2,
        description = "AndroidKeyStore backed key material, verified cryptographic envelopes, user confirmation."
    ),
    HIGH(
        title = "HIGH",
        securityLevel = 3,
        description = "Hardware TEE / StrongBox isolation, biometric confirmation, strict envelope validation."
    );

    val isHwIsolated: Boolean get() = this == STANDARD || this == HIGH
    val isTeeRequired: Boolean get() = this == HIGH
}
