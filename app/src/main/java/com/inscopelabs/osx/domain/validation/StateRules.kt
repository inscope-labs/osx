package com.inscopelabs.osx.domain.validation

import com.inscopelabs.osx.domain.session.model.SessionState
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult

/**
 * CNS-6: State Rules Engine.
 * Verifies that state transitions adhere strictly to allowed state automata graphs.
 */
object StateRules {
    private val allowedSessionTransitions = mapOf(
        SessionState.INITIALIZING to setOf(SessionState.ACTIVE, SessionState.TERMINATED),
        SessionState.ACTIVE to setOf(SessionState.HANDING_OFF, SessionState.TERMINATED),
        SessionState.HANDING_OFF to setOf(SessionState.HANDED_OFF, SessionState.TERMINATED),
        SessionState.HANDED_OFF to setOf(SessionState.TERMINATED),
        SessionState.TERMINATED to emptySet()
    )

    fun canTransitionSession(from: SessionState, to: SessionState): ConsoleResult<Unit> {
        val allowed = allowedSessionTransitions[from] ?: emptySet()
        return if (allowed.contains(to)) {
            ConsoleResult.Success(Unit)
        } else {
            ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS6-ERR-ILLEGAL-TRANSITION",
                    message = "Illegal session state transition from ${from.name} to ${to.name}",
                    cnsSpec = "CNS-6"
                )
            )
        }
    }
}
