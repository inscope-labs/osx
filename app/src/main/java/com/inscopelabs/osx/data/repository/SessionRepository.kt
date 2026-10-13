package com.inscopelabs.osx.data.repository

import com.inscopelabs.osx.data.local.ConsoleDatabase
import com.inscopelabs.osx.data.local.SessionEntity
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.session.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(
    private val database: ConsoleDatabase
) {
    val allSessions: Flow<List<ConsoleSession>> = database.sessionDao().getAllSessions().map { list ->
        list.map { entity ->
            ConsoleSession(
                sessionId = entity.sessionId,
                operatorId = entity.operatorId,
                targetControllerId = entity.targetControllerId,
                profile = OperationalProfile.valueOf(entity.profile),
                state = SessionState.valueOf(entity.state),
                sessionKeyId = entity.sessionKeyId,
                startedAt = entity.startedAt,
                expiresAt = entity.expiresAt,
                lastHeartbeatAt = entity.lastHeartbeatAt,
                handoffTargetConsoleId = entity.handoffTargetConsoleId
            )
        }
    }

    suspend fun saveSession(session: ConsoleSession) {
        database.sessionDao().insertSession(
            SessionEntity(
                sessionId = session.sessionId,
                operatorId = session.operatorId,
                targetControllerId = session.targetControllerId,
                profile = session.profile.name,
                state = session.state.name,
                sessionKeyId = session.sessionKeyId,
                startedAt = session.startedAt,
                expiresAt = session.expiresAt,
                lastHeartbeatAt = session.lastHeartbeatAt,
                handoffTargetConsoleId = session.handoffTargetConsoleId
            )
        )
    }
}
