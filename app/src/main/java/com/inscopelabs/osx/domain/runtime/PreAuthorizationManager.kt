package com.inscopelabs.osx.domain.runtime

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.runtime.model.PreAuthStatus
import com.inscopelabs.osx.domain.runtime.model.PreAuthorization
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import com.inscopelabs.osx.util.sha256
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * CNS-5: Pre-Authorization Manager.
 * Issues and monitors pre-authorized computation budgets, enforcing CNS-1 Invariant 04.
 */
class PreAuthorizationManager {
    private val _tokens = MutableStateFlow<List<PreAuthorization>>(emptyList())
    val tokens: StateFlow<List<PreAuthorization>> = _tokens.asStateFlow()

    init {
        seedDefaultTokens()
    }

    private fun seedDefaultTokens() {
        val token1 = PreAuthorization(
            id = generateId("pa"),
            token = "AUTH-TOKEN-${UUID.randomUUID().toString().take(8).uppercase()}",
            scope = "compute:sec-enclave:standard",
            budgetCapUnits = 1000L,
            remainingUnits = 850L
        )

        val token2 = PreAuthorization(
            id = generateId("pa"),
            token = "AUTH-TOKEN-${UUID.randomUUID().toString().take(8).uppercase()}",
            scope = "batch:wasm:fast",
            budgetCapUnits = 500L,
            remainingUnits = 120L
        )

        _tokens.value = listOf(token1, token2)
    }

    fun issuePreAuthorization(scope: String, budgetCapUnits: Long, durationMs: Long = 1800_000L): PreAuthorization {
        val token = PreAuthorization(
            id = generateId("pa"),
            token = "AUTH-TOKEN-${UUID.randomUUID().toString().take(8).uppercase()}",
            scope = scope,
            budgetCapUnits = budgetCapUnits,
            remainingUnits = budgetCapUnits,
            expiresAt = System.currentTimeMillis() + durationMs
        )
        _tokens.value = listOf(token) + _tokens.value
        return token
    }

    fun debitBudget(tokenId: String, unitsRequested: Long): ConsoleResult<PreAuthorization> {
        val current = _tokens.value
        val target = current.find { it.id == tokenId || it.token == tokenId }
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS5-ERR-TOKEN-NOT-FOUND", "Pre-auth token $tokenId not found", cnsSpec = "CNS-5")
            )

        if (!target.isValid) {
            return ConsoleResult.Failure(
                ConsoleError("CNS5-ERR-TOKEN-INVALID", "Pre-auth token is ${target.status.name} or expired", cnsSpec = "CNS-5")
            )
        }

        // CNS-1 Invariant 04: Budget Cap
        val budgetCheck = InvariantEnforcer.assertCondition(
            ConsoleInvariant.INV_04_PREAUTH_BUDGET_CAP,
            target.remainingUnits >= unitsRequested,
            "Requested $unitsRequested units exceeds remaining balance of ${target.remainingUnits} units"
        )
        if (budgetCheck is ConsoleResult.Failure) return ConsoleResult.Failure(budgetCheck.error)

        val updatedRemaining = target.remainingUnits - unitsRequested
        val updatedStatus = if (updatedRemaining == 0L) PreAuthStatus.EXHAUSTED else PreAuthStatus.ACTIVE

        val updated = target.copy(
            remainingUnits = updatedRemaining,
            status = updatedStatus
        )

        _tokens.value = current.map { if (it.id == target.id) updated else it }
        return ConsoleResult.Success(updated)
    }

    fun revokeToken(tokenId: String): ConsoleResult<PreAuthorization> {
        val current = _tokens.value
        val target = current.find { it.id == tokenId || it.token == tokenId }
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS5-ERR-TOKEN-NOT-FOUND", "Pre-auth token $tokenId not found", cnsSpec = "CNS-5")
            )

        val updated = target.copy(status = PreAuthStatus.REVOKED)
        _tokens.value = current.map { if (it.id == target.id) updated else it }
        return ConsoleResult.Success(updated)
    }
}
