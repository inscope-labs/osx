package com.inscopelabs.osx.domain.envelope.model

import com.inscopelabs.osx.domain.profile.OperationalProfile

/**
 * Concrete payload models for CNS-3 envelopes.
 */
sealed interface EnvelopePayload {
    val schemaVersion: String get() = "1.0.0"
}

data class HandshakePayload(
    val consoleId: String,
    val supportedProfiles: List<OperationalProfile>,
    val clientNonce: String,
    val publicKeyFingerprint: String
) : EnvelopePayload

data class SessionStartPayload(
    val sessionId: String,
    val operatorId: String,
    val selectedProfile: OperationalProfile,
    val targetControllerId: String,
    val sessionTimeoutMs: Long
) : EnvelopePayload

data class RuntimeDiscoveryPayload(
    val runtimeId: String,
    val runtimeName: String,
    val executionEngine: String,
    val supportedProfiles: List<OperationalProfile>,
    val maxConcurrentOrders: Int
) : EnvelopePayload

data class WorkOrderPayload(
    val workOrderId: String,
    val targetRuntimeId: String,
    val operationCode: String,
    val parametersJson: String,
    val preAuthToken: String? = null,
    val maxExecutionTimeMs: Long = 5000L
) : EnvelopePayload

data class PreAuthPayload(
    val preAuthId: String,
    val authorizedScope: String,
    val budgetCapUnits: Long,
    val validUntilTimestamp: Long
) : EnvelopePayload

data class HeartbeatPayload(
    val senderId: String,
    val uptimeMs: Long,
    val cpuLoadPercent: Float,
    val memoryUsageMb: Long
) : EnvelopePayload

data class RevocationPayload(
    val targetIdentifier: String, // key ID or session ID
    val reason: String,
    val effectiveImmediate: Boolean = true
) : EnvelopePayload

data class RawTextPayload(
    val text: String
) : EnvelopePayload
