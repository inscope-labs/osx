package com.inscopelabs.osx.domain.key.model

import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.profile.OperationalProfile

enum class KeyRole(val level: Int, val description: String) {
    ROOT(1, "Console Root Identity Anchor"),
    SIGNING(2, "Session and Envelope Signing Key"),
    SESSION(3, "Ephemeral Transport & Encryption Key"),
    DEVICE(4, "Device Hardware Binding Key")
}

enum class KeyAlgorithm(val standardName: String) {
    HMAC_SHA256("HmacSHA256"),
    AES_256_GCM("AES/GCM/NoPadding"),
    EC_SECP256R1("SHA256withECDSA"),
    ED25519("Ed25519")
}

enum class KeyStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

data class ConsoleKey(
    val id: String,
    val alias: String,
    val role: KeyRole,
    val algorithm: KeyAlgorithm,
    val profile: OperationalProfile,
    val confirmationPolicy: ConfirmationPolicy,
    val status: KeyStatus = KeyStatus.ACTIVE,
    val isHardwareBacked: Boolean = false,
    val publicKeyFingerprint: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L * 30 // 30 days default
)
