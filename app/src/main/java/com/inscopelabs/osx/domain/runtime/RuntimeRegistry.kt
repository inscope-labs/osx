package com.inscopelabs.osx.domain.runtime

import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.runtime.model.ConsoleRuntime
import com.inscopelabs.osx.domain.runtime.model.RuntimeStatus
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CNS-5: Runtime Registry.
 * Discovers, registers, and tracks OS/10 execution engines.
 */
class RuntimeRegistry {
    private val _runtimes = MutableStateFlow<List<ConsoleRuntime>>(emptyList())
    val runtimes: StateFlow<List<ConsoleRuntime>> = _runtimes.asStateFlow()

    init {
        seedDefaultRuntimes()
    }

    private fun seedDefaultRuntimes() {
        val rt1 = ConsoleRuntime(
            id = generateId("rt-sec"),
            name = "Secure Enclave Node Alpha",
            executionEngine = "OS10-TEE-MICROVM",
            endpointUrl = "tls://runtime-alpha.osx.internal:8443",
            status = RuntimeStatus.ONLINE,
            supportedProfiles = listOf(OperationalProfile.STANDARD, OperationalProfile.HIGH),
            maxConcurrentOrders = 8,
            currentActiveOrders = 1
        )

        val rt2 = ConsoleRuntime(
            id = generateId("rt-fast"),
            name = "Edge Compute Worker Beta",
            executionEngine = "OS10-WASM-SANDBOX",
            endpointUrl = "tls://worker-beta.osx.internal:9000",
            status = RuntimeStatus.ONLINE,
            supportedProfiles = listOf(OperationalProfile.BASIC, OperationalProfile.STANDARD),
            maxConcurrentOrders = 16,
            currentActiveOrders = 3
        )

        val rt3 = ConsoleRuntime(
            id = generateId("rt-iso"),
            name = "Air-Gapped HSM Vault Delta",
            executionEngine = "OS10-AIRGAP-HSM",
            endpointUrl = "sec-bus://vault-delta.osx.local:443",
            status = RuntimeStatus.BUSY,
            supportedProfiles = listOf(OperationalProfile.HIGH),
            maxConcurrentOrders = 2,
            currentActiveOrders = 2
        )

        _runtimes.value = listOf(rt1, rt2, rt3)
    }

    fun registerRuntime(runtime: ConsoleRuntime): ConsoleResult<Unit> {
        val current = _runtimes.value
        if (current.any { it.id == runtime.id }) {
            return ConsoleResult.Failure(
                ConsoleError("CNS5-ERR-EXISTS", "Runtime ${runtime.id} already registered", cnsSpec = "CNS-5")
            )
        }
        _runtimes.value = current + runtime
        return ConsoleResult.Success(Unit)
    }

    fun findCapableRuntime(profile: OperationalProfile): ConsoleRuntime? {
        return _runtimes.value.firstOrNull {
            it.status == RuntimeStatus.ONLINE &&
                    it.supportedProfiles.contains(profile) &&
                    it.currentActiveOrders < it.maxConcurrentOrders
        }
    }

    fun updateRuntimeStatus(runtimeId: String, status: RuntimeStatus): ConsoleResult<ConsoleRuntime> {
        val current = _runtimes.value
        val target = current.find { it.id == runtimeId }
            ?: return ConsoleResult.Failure(
                ConsoleError("CNS5-ERR-NOT-FOUND", "Runtime $runtimeId not found", cnsSpec = "CNS-5")
            )

        val updated = target.copy(status = status, lastSeenAt = System.currentTimeMillis())
        _runtimes.value = current.map { if (it.id == runtimeId) updated else it }
        return ConsoleResult.Success(updated)
    }
}
