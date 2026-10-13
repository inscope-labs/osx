package com.inscopelabs.osx.domain.validation

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class RevocationRecord(
    val identifier: String,
    val type: String, // KEY, SESSION, TOKEN, ENVELOPE
    val reason: String,
    val revokedAt: Long = System.currentTimeMillis()
)

/**
 * CNS-6: Revocation Handler.
 * Maintains real-time blacklist for instant invalidation (CNS-1 Invariant 03).
 */
class RevocationHandler {
    private val _revokedEntries = MutableStateFlow<List<RevocationRecord>>(emptyList())
    val revokedEntries: StateFlow<List<RevocationRecord>> = _revokedEntries.asStateFlow()

    private val fastLookup = ConcurrentHashMap<String, RevocationRecord>()

    fun revoke(identifier: String, type: String, reason: String): ConsoleResult<RevocationRecord> {
        val record = RevocationRecord(identifier, type, reason)
        fastLookup[identifier] = record
        _revokedEntries.value = listOf(record) + _revokedEntries.value

        // Assert Invariant 3: Revocation immediacy
        InvariantEnforcer.assertCondition(
            ConsoleInvariant.INV_03_REVOCATION_IMMEDIACY,
            fastLookup.containsKey(identifier),
            "Identifier $identifier must be immediately added to revocation lookup table."
        )

        return ConsoleResult.Success(record)
    }

    fun isRevoked(identifier: String): Boolean {
        return fastLookup.containsKey(identifier)
    }

    fun getRevocationReason(identifier: String): String? {
        return fastLookup[identifier]?.reason
    }
}
