package com.inscopelabs.osx.domain.envelope

import com.inscopelabs.osx.domain.profile.OperationalProfile

enum class EnvelopeStatus {
    VERIFIED,
    UNVERIFIED,
    SIGNATURE_INVALID,
    EXPIRED,
    REPLAYED,
    REVOKED
}

data class EnvelopeHeader(
    val id: String,
    val type: EnvelopeType,
    val senderId: String,
    val recipientId: String,
    val timestamp: Long,
    val nonce: String,
    val profile: OperationalProfile,
    val signerKeyId: String? = null
)

data class Envelope(
    val header: EnvelopeHeader,
    val payloadRaw: String,
    val digest: String,
    val signature: String? = null,
    val status: EnvelopeStatus = EnvelopeStatus.UNVERIFIED
) {
    val isVerified: Boolean get() = status == EnvelopeStatus.VERIFIED
}
