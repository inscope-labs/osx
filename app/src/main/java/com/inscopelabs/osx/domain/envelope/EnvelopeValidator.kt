package com.inscopelabs.osx.domain.envelope

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.profile.ProfileRequirements
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.sha256
import java.util.concurrent.ConcurrentHashMap

/**
 * CNS-3 & CNS-1: Envelope Validator.
 * Validates integrity, freshness, replay protection, and signatures.
 */
class EnvelopeValidator {
    // Monotonic replay prevention cache (nonce -> timestamp seen)
    private val seenNonces = ConcurrentHashMap<String, Long>()
    private val clockSkewToleranceMs = 5000L

    fun computeDigest(header: EnvelopeHeader, payloadRaw: String): String {
        val canonical = "${header.id}|${header.type.name}|${header.senderId}|${header.recipientId}|${header.timestamp}|${header.nonce}|${header.profile.name}|${header.signerKeyId.orEmpty()}|$payloadRaw"
        return canonical.sha256()
    }

    fun validateEnvelope(
        envelope: Envelope,
        isSignerRevoked: Boolean = false,
        signatureVerifier: ((keyId: String, digest: String, signature: String) -> Boolean)? = null
    ): ConsoleResult<Envelope> {
        val now = System.currentTimeMillis()
        val header = envelope.header
        val requirements = ProfileRequirements.forProfile(header.profile)

        // 1. Clock freshness check
        if (header.timestamp > now + clockSkewToleranceMs) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS3-ERR-FUTURE-TS",
                    message = "Envelope timestamp is in the future: ${header.timestamp} vs now $now",
                    cnsSpec = "CNS-3"
                )
            )
        }

        if (now - header.timestamp > requirements.maxEnvelopeTtlMs) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS3-ERR-EXPIRED",
                    message = "Envelope expired for profile ${header.profile.name}. Age: ${now - header.timestamp}ms > Max: ${requirements.maxEnvelopeTtlMs}ms",
                    cnsSpec = "CNS-3"
                )
            )
        }

        // 2. CNS-1 Invariant 05: Replay Protection
        if (requirements.enforceReplayCache) {
            if (seenNonces.containsKey(header.nonce)) {
                return ConsoleResult.Failure(
                    ConsoleError(
                        code = ConsoleInvariant.INV_05_REPLAY_PROTECTION.code,
                        message = "Replay detected: Nonce ${header.nonce} has already been consumed.",
                        cnsSpec = "CNS-1"
                    )
                )
            }
            seenNonces[header.nonce] = now
            // Evict nonces older than max TTL * 2
            val cutoff = now - (requirements.maxEnvelopeTtlMs * 2)
            seenNonces.entries.removeIf { it.value < cutoff }
        }

        // 3. Digest verification
        val expectedDigest = computeDigest(header, envelope.payloadRaw)
        if (expectedDigest != envelope.digest) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS3-ERR-DIGEST-MISMATCH",
                    message = "Digest mismatch! Expected: $expectedDigest, got: ${envelope.digest}",
                    cnsSpec = "CNS-3"
                )
            )
        }

        // 4. Invariant 03: Revocation check
        if (isSignerRevoked) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = ConsoleInvariant.INV_03_REVOCATION_IMMEDIACY.code,
                    message = "Signer key ${header.signerKeyId} is revoked.",
                    cnsSpec = "CNS-1"
                )
            )
        }

        // 5. Signature verification
        if (header.type.requiresSigner) {
            if (envelope.signature.isNullOrBlank() || header.signerKeyId.isNullOrBlank()) {
                return ConsoleResult.Failure(
                    ConsoleError(
                        code = ConsoleInvariant.INV_01_CRYPTO_AUTHENTICITY.code,
                        message = "Envelope type ${header.type.name} requires cryptographic signature and signer key.",
                        cnsSpec = "CNS-1"
                    )
                )
            }

            if (signatureVerifier != null) {
                val isValidSig = signatureVerifier(header.signerKeyId, envelope.digest, envelope.signature)
                if (!isValidSig) {
                    return ConsoleResult.Failure(
                        ConsoleError(
                            code = "CNS3-ERR-SIGNATURE-INVALID",
                            message = "Cryptographic signature failed verification for key ${header.signerKeyId}",
                            cnsSpec = "CNS-3"
                        )
                    )
                }
            }
        }

        return ConsoleResult.Success(envelope.copy(status = EnvelopeStatus.VERIFIED))
    }
}
