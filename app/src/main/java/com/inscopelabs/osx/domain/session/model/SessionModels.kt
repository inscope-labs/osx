package com.inscopelabs.osx.domain.session.model

import com.inscopelabs.osx.domain.profile.OperationalProfile

enum class SessionState {
    INITIALIZING,
    ACTIVE,
    HANDING_OFF,
    HANDED_OFF,
    TERMINATED
}

data class ConsoleSession(
    val sessionId: String,
    val operatorId: String,
    val targetControllerId: String,
    val profile: OperationalProfile,
    val state: SessionState = SessionState.INITIALIZING,
    val sessionKeyId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 3600_000L, // 1 hr default
    val lastHeartbeatAt: Long = System.currentTimeMillis(),
    val handoffTargetConsoleId: String? = null
) {
    val isActive: Boolean get() = state == SessionState.ACTIVE
}

data class HandoffRequest(
    val sessionId: String,
    val sourceConsoleId: String,
    val targetConsoleId: String,
    val profile: OperationalProfile,
    val sessionStateDigest: String,
    val operatorConfirmationToken: String
)

data class HandoffResult(
    val success: Boolean,
    val handoffId: String,
    val newSessionId: String? = null,
    val message: String
)
