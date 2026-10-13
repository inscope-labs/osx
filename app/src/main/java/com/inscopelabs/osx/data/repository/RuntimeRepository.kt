package com.inscopelabs.osx.data.repository

import com.inscopelabs.osx.data.local.ConsoleDatabase
import com.inscopelabs.osx.data.local.RuntimeEntity
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.runtime.model.ConsoleRuntime
import com.inscopelabs.osx.domain.runtime.model.RuntimeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuntimeRepository(
    private val database: ConsoleDatabase
) {
    val allRuntimes: Flow<List<ConsoleRuntime>> = database.runtimeDao().getAllRuntimes().map { list ->
        list.map { entity ->
            ConsoleRuntime(
                id = entity.id,
                name = entity.name,
                executionEngine = entity.executionEngine,
                endpointUrl = entity.endpointUrl,
                status = RuntimeStatus.valueOf(entity.status),
                supportedProfiles = entity.supportedProfilesCsv.split(",").mapNotNull {
                    try { OperationalProfile.valueOf(it.trim()) } catch (e: Exception) { null }
                },
                maxConcurrentOrders = entity.maxConcurrentOrders,
                currentActiveOrders = entity.currentActiveOrders,
                lastSeenAt = entity.lastSeenAt
            )
        }
    }

    suspend fun saveRuntime(runtime: ConsoleRuntime) {
        database.runtimeDao().insertRuntime(
            RuntimeEntity(
                id = runtime.id,
                name = runtime.name,
                executionEngine = runtime.executionEngine,
                endpointUrl = runtime.endpointUrl,
                status = runtime.status.name,
                supportedProfilesCsv = runtime.supportedProfiles.joinToString(",") { it.name },
                maxConcurrentOrders = runtime.maxConcurrentOrders,
                currentActiveOrders = runtime.currentActiveOrders,
                lastSeenAt = runtime.lastSeenAt
            )
        )
    }
}
