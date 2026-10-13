package com.inscopelabs.osx.domain.key

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.key.model.ConsoleKey
import com.inscopelabs.osx.domain.key.model.KeyAlgorithm
import com.inscopelabs.osx.domain.key.model.KeyRole
import com.inscopelabs.osx.domain.key.model.KeyStatus
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import com.inscopelabs.osx.util.sha256
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CNS-2: Console Key Manager.
 * Orchestrates key lifecycle, rotation, and authorization across operational profiles.
 */
class ConsoleKeyManager(
    private val keyHierarchy: KeyHierarchy = KeyHierarchy()
) {
    private val _keys = MutableStateFlow<List<ConsoleKey>>(emptyList())
    val keys: StateFlow<List<ConsoleKey>> = _keys.asStateFlow()

    init {
        // Seed default Root and Signing keys
        seedDefaultKeys()
    }

    private fun seedDefaultKeys() {
        val rootKey = ConsoleKey(
            id = generateId("key-root"),
            alias = "osx.root.primary",
            role = KeyRole.ROOT,
            algorithm = KeyAlgorithm.EC_SECP256R1,
            profile = OperationalProfile.HIGH,
            confirmationPolicy = ConfirmationPolicy.STEP_UP_AUTHENTICATED,
            status = KeyStatus.ACTIVE,
            isHardwareBacked = true,
            publicKeyFingerprint = "7a3f89e1b4c902d84410a8d3e69f88c2".sha256().take(16)
        )

        val signingKey = ConsoleKey(
            id = generateId("key-sign"),
            alias = "osx.signing.standard",
            role = KeyRole.SIGNING,
            algorithm = KeyAlgorithm.EC_SECP256R1,
            profile = OperationalProfile.STANDARD,
            confirmationPolicy = ConfirmationPolicy.USER_CONFIRMATION,
            status = KeyStatus.ACTIVE,
            isHardwareBacked = true,
            publicKeyFingerprint = "1b4c902d84410a8d3e69f88c27a3f89e".sha256().take(16)
        )

        val sessionKey = ConsoleKey(
            id = generateId("key-sess"),
            alias = "osx.session.ephemeral",
            role = KeyRole.SESSION,
            algorithm = KeyAlgorithm.AES_256_GCM,
            profile = OperationalProfile.BASIC,
            confirmationPolicy = ConfirmationPolicy.NONE,
            status = KeyStatus.ACTIVE,
            isHardwareBacked = false,
            publicKeyFingerprint = "02d84410a8d3e69f88c27a3f89e1b4c9".sha256().take(16)
        )

        _keys.value = listOf(rootKey, signingKey, sessionKey)
    }

    fun getActiveKey(role: KeyRole): ConsoleKey? {
        return _keys.value.firstOrNull { it.role == role && it.status == KeyStatus.ACTIVE }
    }

    fun registerKey(key: ConsoleKey): ConsoleResult<Unit> {
        val current = _keys.value
        if (current.any { it.id == key.id }) {
            return ConsoleResult.Failure(
                ConsoleError("CNS2-ERR-EXISTS", "Key ID ${key.id} already registered", cnsSpec = "CNS-2")
            )
        }
        _keys.value = current + key
        return ConsoleResult.Success(Unit)
    }

    fun revokeKey(keyId: String, reason: String): ConsoleResult<ConsoleKey> {
        val current = _keys.value
        val target = current.find { it.id == keyId }
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS2-ERR-NOT-FOUND", "Key $keyId not found", cnsSpec = "CNS-2")
            )

        val updated = target.copy(status = KeyStatus.REVOKED)
        _keys.value = current.map { if (it.id == keyId) updated else it }

        // Assert invariant 3 (Revocation Immediacy)
        InvariantEnforcer.assertCondition(
            ConsoleInvariant.INV_03_REVOCATION_IMMEDIACY,
            updated.status == KeyStatus.REVOKED,
            "Key $keyId must immediately enter revoked state."
        )

        return ConsoleResult.Success(updated)
    }

    fun rotateSigningKey(profile: OperationalProfile): ConsoleResult<ConsoleKey> {
        val newKey = ConsoleKey(
            id = generateId("key-sign"),
            alias = "osx.signing.${profile.name.lowercase()}.${System.currentTimeMillis() % 10000}",
            role = KeyRole.SIGNING,
            algorithm = if (profile == OperationalProfile.BASIC) KeyAlgorithm.HMAC_SHA256 else KeyAlgorithm.EC_SECP256R1,
            profile = profile,
            confirmationPolicy = if (profile == OperationalProfile.HIGH) ConfirmationPolicy.STEP_UP_AUTHENTICATED else ConfirmationPolicy.USER_CONFIRMATION,
            status = KeyStatus.ACTIVE,
            isHardwareBacked = profile.isHwIsolated,
            publicKeyFingerprint = generateId("fp").sha256().take(16)
        )

        val current = _keys.value
        // Deprecate previous signing key of same profile
        val updated = current.map {
            if (it.role == KeyRole.SIGNING && it.profile == profile && it.status == KeyStatus.ACTIVE) {
                it.copy(status = KeyStatus.EXPIRED)
            } else it
        }
        _keys.value = updated + newKey
        return ConsoleResult.Success(newKey)
    }
}
