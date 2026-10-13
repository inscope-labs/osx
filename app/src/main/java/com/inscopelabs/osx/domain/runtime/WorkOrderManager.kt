package com.inscopelabs.osx.domain.runtime

import com.inscopelabs.osx.domain.runtime.model.ConsoleRuntime
import com.inscopelabs.osx.domain.runtime.model.WorkOrder
import com.inscopelabs.osx.domain.runtime.model.WorkOrderStatus
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CNS-5: Work Order Manager.
 * Orchestrates work order lifecycle, runtime dispatch, and outcome logging.
 */
class WorkOrderManager(
    private val preAuthManager: PreAuthorizationManager
) {
    private val _workOrders = MutableStateFlow<List<WorkOrder>>(emptyList())
    val workOrders: StateFlow<List<WorkOrder>> = _workOrders.asStateFlow()

    init {
        seedDefaultOrders()
    }

    private fun seedDefaultOrders() {
        val o1 = WorkOrder(
            id = generateId("wo"),
            targetRuntimeId = "rt-sec-enclave",
            operation = "SECURITY_AUDIT_PASS",
            parameters = "{\"depth\": \"exhaustive\", \"verifyEnclaves\": true}",
            budgetUnits = 150L,
            status = WorkOrderStatus.COMPLETED,
            submittedAt = System.currentTimeMillis() - 120_000L,
            completedAt = System.currentTimeMillis() - 60_000L,
            resultSummary = "Audit completed: 0 violations, 412 invariants intact"
        )
        _workOrders.value = listOf(o1)
    }

    fun submitWorkOrder(
        targetRuntime: ConsoleRuntime,
        operation: String,
        parameters: String,
        budgetUnits: Long,
        preAuthTokenId: String? = null
    ): ConsoleResult<WorkOrder> {
        // If a pre-auth token is provided, debit budget
        if (!preAuthTokenId.isNullOrBlank()) {
            val debitResult = preAuthManager.debitBudget(preAuthTokenId, budgetUnits)
            if (debitResult is ConsoleResult.Failure) {
                val failedOrder = WorkOrder(
                    id = generateId("wo"),
                    targetRuntimeId = targetRuntime.id,
                    operation = operation,
                    parameters = parameters,
                    budgetUnits = budgetUnits,
                    preAuthToken = preAuthTokenId,
                    status = WorkOrderStatus.BUDGET_EXCEEDED,
                    resultSummary = "Rejected: ${debitResult.error.message}"
                )
                _workOrders.value = listOf(failedOrder) + _workOrders.value
                return ConsoleResult.Failure(debitResult.error)
            }
        }

        val order = WorkOrder(
            id = generateId("wo"),
            targetRuntimeId = targetRuntime.id,
            operation = operation,
            parameters = parameters,
            budgetUnits = budgetUnits,
            preAuthToken = preAuthTokenId,
            status = WorkOrderStatus.EXECUTING
        )

        _workOrders.value = listOf(order) + _workOrders.value

        // Simulate immediate execution for console demonstration
        val completed = order.copy(
            status = WorkOrderStatus.COMPLETED,
            completedAt = System.currentTimeMillis() + 850L,
            resultSummary = "Successfully executed on ${targetRuntime.name} in 850ms"
        )
        _workOrders.value = _workOrders.value.map { if (it.id == order.id) completed else it }

        return ConsoleResult.Success(completed)
    }
}
