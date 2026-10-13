package com.inscopelabs.osx.data.network

import com.inscopelabs.osx.domain.profile.OperationalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredController(
    val controllerId: String,
    val hostname: String,
    val port: Int,
    val tlsFingerprint: String,
    val supportedProfiles: List<OperationalProfile>,
    val latencyMs: Long
)

/**
 * Discovery Client for finding local and mesh OS/10 controller beacons.
 */
class DiscoveryClient {
    private val _controllers = MutableStateFlow<List<DiscoveredController>>(emptyList())
    val controllers: StateFlow<List<DiscoveredController>> = _controllers.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        _controllers.value = listOf(
            DiscoveredController(
                controllerId = "CTRL-CORE-01",
                hostname = "ctrl-primary.local",
                port = 9443,
                tlsFingerprint = "SHA256:7D:9A:31:BC:E4:01:88:2F:AA:09",
                supportedProfiles = listOf(OperationalProfile.STANDARD, OperationalProfile.HIGH),
                latencyMs = 14
            ),
            DiscoveredController(
                controllerId = "CTRL-BACKUP-02",
                hostname = "ctrl-standby.local",
                port = 9443,
                tlsFingerprint = "SHA256:4C:11:98:AA:66:32:01:FE:8B:11",
                supportedProfiles = listOf(OperationalProfile.BASIC, OperationalProfile.STANDARD),
                latencyMs = 28
            )
        )
    }
}
