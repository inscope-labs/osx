package com.inscopelabs.osx.domain.foundation

import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult

/**
 * CNS-1: Core Invariants of the OS/10 Console.
 * Invariants represent non-negotiable security guarantees.
 */
enum class ConsoleInvariant(val code: String, val description: String) {
    INV_01_CRYPTO_AUTHENTICITY(
        "INV-01",
        "Payloads crossing the trust boundary must have verifiable cryptographic signatures."
    ),
    INV_02_PROFILE_STEP_UP_INTEGRITY(
        "INV-02",
        "Operational profile escalation requires step-up hardware confirmation."
    ),
    INV_03_REVOCATION_IMMEDIACY(
        "INV-03",
        "Revoked keys or superseded session tokens are instantly rejected."
    ),
    INV_04_PREAUTH_BUDGET_CAP(
        "INV-04",
        "Work orders cannot exceed pre-authorized computational and policy budgets."
    ),
    INV_05_REPLAY_PROTECTION(
        "INV-05",
        "Nonce identifiers are monotonically tracked and replay attacks are rejected."
    )
}

data class InvariantViolation(
    val invariant: ConsoleInvariant,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

object InvariantEnforcer {
    fun assertCondition(
        invariant: ConsoleInvariant,
        condition: Boolean,
        message: String
    ): ConsoleResult<Unit> {
        return if (condition) {
            ConsoleResult.Success(Unit)
        } else {
            ConsoleResult.Failure(
                ConsoleError(
                    code = invariant.code,
                    message = "CNS-1 Violation [${invariant.code}]: $message (${invariant.description})",
                    cnsSpec = "CNS-1"
                )
            )
        }
    }
}
