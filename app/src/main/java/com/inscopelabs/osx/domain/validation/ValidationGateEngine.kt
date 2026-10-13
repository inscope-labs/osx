package com.inscopelabs.osx.domain.validation

import com.inscopelabs.osx.domain.envelope.Envelope
import com.inscopelabs.osx.domain.envelope.EnvelopeStatus
import com.inscopelabs.osx.domain.envelope.EnvelopeValidator
import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.foundation.TrustBoundary
import com.inscopelabs.osx.domain.foundation.TrustZone
import com.inscopelabs.osx.domain.key.ConsoleKeyManager
import com.inscopelabs.osx.domain.key.model.KeyStatus
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GateAuditEntry(
    val id: String = generateId("gate"),
    val targetId: String,
    val envelopeType: String,
    val passed: Boolean,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * CNS-6: Validation Gate Engine.
 * Multi-stage pipeline filtering incoming and outgoing console transmissions.
 */
class ValidationGateEngine(
    private val envelopeValidator: EnvelopeValidator,
    private val revocationHandler: RevocationHandler,
    private val trustBoundary: TrustBoundary,
    private val keyManager: ConsoleKeyManager
) {
    private val _auditLogs = MutableStateFlow<List<GateAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<GateAuditEntry>> = _auditLogs.asStateFlow()

    fun evaluateEnvelope(
        envelope: Envelope,
        sourceZone: TrustZone = TrustZone.EXTERNAL_UNTRUSTED,
        targetZone: TrustZone = TrustZone.CONSOLE_CORE
    ): ConsoleResult<Envelope> {
        // Stage 1: Trust boundary inspection
        val boundaryCheck = trustBoundary.inspectTransition(
            from = sourceZone,
            to = targetZone,
            hasValidEnvelope = true,
            hasHardwareProtection = envelope.header.profile.isHwIsolated
        )
        if (boundaryCheck is ConsoleResult.Failure) {
            recordAudit(envelope.header.id, envelope.header.type.name, false, "Boundary violation: ${boundaryCheck.error.message}")
            return ConsoleResult.Failure(boundaryCheck.error)
        }

        // Stage 2: Revocation check on envelope and signer key
        if (revocationHandler.isRevoked(envelope.header.id)) {
            val reason = revocationHandler.getRevocationReason(envelope.header.id)
            recordAudit(envelope.header.id, envelope.header.type.name, false, "Envelope is revoked: $reason")
            return ConsoleResult.Failure(
                ConsoleError("CNS6-ERR-REVOKED", "Envelope ID ${envelope.header.id} is revoked", cnsSpec = "CNS-6")
            )
        }

        val signerKeyId = envelope.header.signerKeyId
        val isSignerRevoked = if (signerKeyId != null) {
            revocationHandler.isRevoked(signerKeyId) ||
                    keyManager.keys.value.find { it.id == signerKeyId }?.status == KeyStatus.REVOKED
        } else false

        if (isSignerRevoked) {
            recordAudit(envelope.header.id, envelope.header.type.name, false, "Signer key $signerKeyId is revoked")
            return ConsoleResult.Failure(
                ConsoleError(
                    code = ConsoleInvariant.INV_03_REVOCATION_IMMEDIACY.code,
                    message = "Signer key $signerKeyId is in REVOKED state",
                    cnsSpec = "CNS-1"
                )
            )
        }

        // Stage 3: Envelope cryptographic and freshness validation
        val validationResult = envelopeValidator.validateEnvelope(
            envelope = envelope,
            isSignerRevoked = isSignerRevoked
        )

        return when (validationResult) {
            is ConsoleResult.Success -> {
                recordAudit(envelope.header.id, envelope.header.type.name, true, "Gate cleared: All invariants & signatures verified")
                ConsoleResult.Success(validationResult.data)
            }
            is ConsoleResult.Failure -> {
                recordAudit(envelope.header.id, envelope.header.type.name, false, "Validation rejected: ${validationResult.error.message}")
                ConsoleResult.Failure(validationResult.error)
            }
        }
    }

    private fun recordAudit(targetId: String, type: String, passed: Boolean, details: String) {
        val entry = GateAuditEntry(
            targetId = targetId,
            envelopeType = type,
            passed = passed,
            details = details
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value.take(99)
    }
}
