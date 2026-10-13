package com.inscopelabs.osx.domain.session

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.session.model.HandoffRequest
import com.inscopelabs.osx.domain.session.model.HandoffResult
import com.inscopelabs.osx.domain.session.model.SessionState
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import com.inscopelabs.osx.util.sha256

/**
 * CNS-4: Session Handoff Handler.
 * Governs state handoffs between operators and console nodes.
 */
class HandoffHandler {
    fun executeHandoff(
        currentSession: ConsoleSession,
        request: HandoffRequest
    ): ConsoleResult<Pair<ConsoleSession, HandoffResult>> {
        if (currentSession.state != SessionState.ACTIVE) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS4-ERR-NOT-ACTIVE",
                    message = "Cannot hand off session in state ${currentSession.state.name}",
                    cnsSpec = "CNS-4"
                )
            )
        }

        if (request.operatorConfirmationToken.isBlank()) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS4-ERR-NO-CONFIRM-TOKEN",
                    message = "Handoff request missing operator confirmation token.",
                    cnsSpec = "CNS-4"
                )
            )
        }

        val handoffId = generateId("hndf")
        val newSessionId = generateId("sess-hndf")

        val handedOffSession = currentSession.copy(
            state = SessionState.HANDED_OFF,
            handoffTargetConsoleId = request.targetConsoleId
        )

        // Enforce Invariant 3: superseding means old session cannot be active
        InvariantEnforcer.assertCondition(
            ConsoleInvariant.INV_03_REVOCATION_IMMEDIACY,
            handedOffSession.state == SessionState.HANDED_OFF,
            "Old session must immediately cease to be ACTIVE upon handoff."
        )

        val result = HandoffResult(
            success = true,
            handoffId = handoffId,
            newSessionId = newSessionId,
            message = "Session ${currentSession.sessionId} successfully handed off to ${request.targetConsoleId}"
        )

        return ConsoleResult.Success(Pair(handedOffSession, result))
    }
}
