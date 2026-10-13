package com.inscopelabs.osx.domain.runtime.model

import com.inscopelabs.osx.domain.profile.OperationalProfile

enum class RuntimeStatus {
    ONLINE,
    BUSY,
    OFFLINE
}

data class ConsoleRuntime(
    val id: String,
    val name: String,
    val executionEngine: String,
    val endpointUrl: String,
    val status: RuntimeStatus = RuntimeStatus.ONLINE,
    val supportedProfiles: List<OperationalProfile>,
    val maxConcurrentOrders: Int = 4,
    val currentActiveOrders: Int = 0,
    val lastSeenAt: Long = System.currentTimeMillis()
)

enum class WorkOrderStatus {
    PENDING,
    DISPATCHED,
    EXECUTING,
    COMPLETED,
    FAILED,
    BUDGET_EXCEEDED
}

data class WorkOrder(
    val id: String,
    val targetRuntimeId: String,
    val operation: String,
    val parameters: String,
    val budgetUnits: Long,
    val preAuthToken: String? = null,
    val status: WorkOrderStatus = WorkOrderStatus.PENDING,
    val submittedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val resultSummary: String? = null
)

enum class PreAuthStatus {
    ACTIVE,
    EXHAUSTED,
    EXPIRED,
    REVOKED
}

data class PreAuthorization(
    val id: String,
    val token: String,
    val scope: String,
    val budgetCapUnits: Long,
    val remainingUnits: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 1800_000L, // 30 min default
    val status: PreAuthStatus = PreAuthStatus.ACTIVE
) {
    val isValid: Boolean get() = status == PreAuthStatus.ACTIVE && remainingUnits > 0 && expiresAt > System.currentTimeMillis()
}
