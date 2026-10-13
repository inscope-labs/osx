package com.inscopelabs.osx.domain.envelope

import com.inscopelabs.osx.domain.key.model.ConsoleKey
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.util.generateId
import java.util.UUID

/**
 * CNS-3: Envelope Factory.
 * Assembles and seals cryptographic envelopes for console transmission.
 */
class EnvelopeFactory(
    private val validator: EnvelopeValidator = EnvelopeValidator()
) {
    fun createEnvelope(
        type: EnvelopeType,
        senderId: String,
        recipientId: String,
        payloadRaw: String,
        profile: OperationalProfile,
        signerKey: ConsoleKey? = null,
        signer: ((key: ConsoleKey, digest: String) -> String)? = null
    ): Envelope {
        val id = generateId("env")
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()

        val header = EnvelopeHeader(
            id = id,
            type = type,
            senderId = senderId,
            recipientId = recipientId,
            timestamp = timestamp,
            nonce = nonce,
            profile = profile,
            signerKeyId = signerKey?.id
        )

        val digest = validator.computeDigest(header, payloadRaw)

        val signature = if (signerKey != null && signer != null) {
            signer(signerKey, digest)
        } else if (signerKey != null) {
            // Default mock cryptographic signature derived from key fingerprint and digest
            "SIG-${signerKey.publicKeyFingerprint.take(8)}-${digest.take(16)}"
        } else null

        return Envelope(
            header = header,
            payloadRaw = payloadRaw,
            digest = digest,
            signature = signature,
            status = EnvelopeStatus.UNVERIFIED
        )
    }
}
