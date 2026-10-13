package com.inscopelabs.osx.domain.envelope

/**
 * CNS-3: Envelope Types.
 * Discrete message classes supported by the OS/10 console envelope interface.
 */
enum class EnvelopeType(
    val code: String,
    val requiresSigner: Boolean,
    val description: String
) {
    HANDSHAKE("ENV-01", true, "Cryptographic capability negotiation and identity binding"),
    SESSION_START("ENV-02", true, "Initiation of a secure operator session"),
    RUNTIME_DISCOVERY("ENV-03", false, "Runtime node capability advertisement"),
    WORK_ORDER("ENV-04", true, "Dispatch of authorized work order payload"),
    PRE_AUTH("ENV-05", true, "Issuance of cryptographic pre-authorization budget"),
    HEARTBEAT("ENV-06", false, "Keepalive ping and node status telemetry"),
    REVOCATION("ENV-07", true, "Immediate revocation of keys or session tokens")
}
