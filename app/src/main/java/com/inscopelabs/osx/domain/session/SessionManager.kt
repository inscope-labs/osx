package com.inscopelabs.osx.domain.session

import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.session.model.HandoffRequest
import com.inscopelabs.osx.domain.session.model.HandoffResult
import com.inscopelabs.osx.domain.session.model.SessionState
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.sha256
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CNS-4: Session Lifecycle Manager.
 * Orchestrates current session, heartbeat updates, handoff transitions, and terminations.
 */
class SessionManager(
    private val sessionStart: SessionStart,
    private val handoffHandler: HandoffHandler = HandoffHandler()
) {
    private val _activeSession = MutableStateFlow<ConsoleSession?>(null)
    val activeSession: StateFlow<ConsoleSession?> = _activeSession.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<ConsoleSession>>(emptyList())
    val sessionHistory: StateFlow<List<ConsoleSession>> = _sessionHistory.asStateFlow()

    fun startSession(
        operatorId: String,
        targetControllerId: String,
        profile: OperationalProfile,
        policy: ConfirmationPolicy
    ): ConsoleResult<ConsoleSession> {
        val result = sessionStart.initiateSession(operatorId, targetControllerId, profile, policy)
        return when (result) {
            is ConsoleResult.Success -> {
                _activeSession.value = result.data
                _sessionHistory.value = listOf(result.data) + _sessionHistory.value
                ConsoleResult.Success(result.data)
            }
            is ConsoleResult.Failure -> ConsoleResult.Failure(result.error)
        }
    }

    fun recordHeartbeat(): ConsoleResult<Unit> {
        val current = _activeSession.value
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS4-ERR-NO-SESSION", "No active session to heartbeat", cnsSpec = "CNS-4")
            )
        val updated = current.copy(lastHeartbeatAt = System.currentTimeMillis())
        _activeSession.value = updated
        _sessionHistory.value = _sessionHistory.value.map { if (it.sessionId == updated.sessionId) updated else it }
        return ConsoleResult.Success(Unit)
    }

    fun terminateSession(reason: String = "Operator logout"): ConsoleResult<ConsoleSession> {
        val current = _activeSession.value
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS4-ERR-NO-SESSION", "No active session to terminate", cnsSpec = "CNS-4")
            )
        val terminated = current.copy(state = SessionState.TERMINATED)
        _activeSession.value = null
        _sessionHistory.value = _sessionHistory.value.map { if (it.sessionId == terminated.sessionId) terminated else it }
        return ConsoleResult.Success(terminated)
    }

    fun requestHandoff(
        targetConsoleId: String,
        confirmationToken: String
    ): ConsoleResult<HandoffResult> {
        val current = _activeSession.value
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS4-ERR-NO-SESSION", "No active session to handoff", cnsSpec = "CNS-4")
            )

        val request = HandoffRequest(
            sessionId = current.sessionId,
            sourceConsoleId = "osx-console-local",
            targetConsoleId = targetConsoleId,
            profile = current.profile,
            sessionStateDigest = "${current.sessionId}|${current.lastHeartbeatAt}".sha256(),
            operatorConfirmationToken = confirmationToken
        )

        val handoffResult = handoffHandler.executeHandoff(current, request)
        return when (handoffResult) {
            is ConsoleResult.Success -> {
                val (handedOff, result) = handoffResult.data
                _activeSession.value = null
                _sessionHistory.value = _sessionHistory.value.map { if (it.sessionId == handedOff.sessionId) handedOff else it }
                ConsoleResult.Success(result)
            }
            is ConsoleResult.Failure -> ConsoleResult.Failure(handoffResult.error)
        }
    }
}
