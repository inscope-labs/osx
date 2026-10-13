package com.inscopelabs.osx.domain.foundation

import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult

/**
 * CNS-1: Trust Boundary Architecture.
 * Separates untrusted exterior transport from the secure internal console core.
 */
enum class TrustZone {
    EXTERNAL_UNTRUSTED,    // Remote controllers, discovery network, raw sockets
    BOUNDARY_GATEWAY,      // Serializer, ingress filter, envelope validation pipeline
    CONSOLE_CORE,          // Key manager, session state machine, profile executor
    HARDWARE_ISOLATION     // AndroidKeyStore, StrongBox / TEE
}

data class BoundaryInspectionResult(
    val permitted: Boolean,
    val sourceZone: TrustZone,
    val targetZone: TrustZone,
    val reason: String
)

class TrustBoundary {
    fun inspectTransition(
        from: TrustZone,
        to: TrustZone,
        hasValidEnvelope: Boolean,
        hasHardwareProtection: Boolean
    ): ConsoleResult<BoundaryInspectionResult> {
        // Direct transition from external to core is prohibited without gateway validation
        if (from == TrustZone.EXTERNAL_UNTRUSTED && to == TrustZone.CONSOLE_CORE) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS1-ERR-BYPASS",
                    message = "Direct perimeter transit from EXTERNAL to CONSOLE_CORE is forbidden.",
                    cnsSpec = "CNS-1"
                )
            )
        }

        if (to == TrustZone.CONSOLE_CORE && !hasValidEnvelope) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS1-ERR-INVALID-ENVELOPE",
                    message = "Transit to CONSOLE_CORE rejected: Envelope authenticity check failed.",
                    cnsSpec = "CNS-1"
                )
            )
        }

        if (to == TrustZone.HARDWARE_ISOLATION && !hasHardwareProtection) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS1-ERR-HW-ISOLATION",
                    message = "Hardware isolation requested but hardware backed keystore unavailable.",
                    cnsSpec = "CNS-1"
                )
            )
        }

        return ConsoleResult.Success(
            BoundaryInspectionResult(
                permitted = true,
                sourceZone = from,
                targetZone = to,
                reason = "Verified boundary transition"
            )
        )
    }
}
