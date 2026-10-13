package com.inscopelabs.osx.data.repository

import com.inscopelabs.osx.data.local.AuditEntity
import com.inscopelabs.osx.data.local.ConsoleDatabase
import com.inscopelabs.osx.domain.validation.GateAuditEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuditRepository(
    private val database: ConsoleDatabase
) {
    val recentAudits: Flow<List<GateAuditEntry>> = database.auditDao().getRecentAudits().map { list ->
        list.map { entity ->
            GateAuditEntry(
                id = entity.id,
                targetId = entity.targetId,
                envelopeType = entity.envelopeType,
                passed = entity.passed,
                details = entity.details,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun logAudit(entry: GateAuditEntry) {
        database.auditDao().insertAudit(
            AuditEntity(
                id = entry.id,
                targetId = entry.targetId,
                envelopeType = entry.envelopeType,
                passed = entry.passed,
                details = entry.details,
                timestamp = entry.timestamp
            )
        )
    }
}
