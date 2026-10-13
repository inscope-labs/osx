package com.inscopelabs.osx.domain.profile

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CNS-7: Profile Selector logic.
 * Manages active operational profile and validates switching constraints.
 */
class ProfileSelector(
    initialProfile: OperationalProfile = OperationalProfile.STANDARD
) {
    private val _currentProfile = MutableStateFlow(initialProfile)
    val currentProfile: StateFlow<OperationalProfile> = _currentProfile.asStateFlow()

    fun canSwitchTo(
        target: OperationalProfile,
        hardwareKeystoreAvailable: Boolean,
        teeAvailable: Boolean,
        providedPolicy: ConfirmationPolicy
    ): ConsoleResult<Unit> {
        val current = _currentProfile.value
        val requirements = ProfileRequirements.forProfile(target)

        // Enforce CNS-1 Invariant 02: Profile step up integrity
        if (target.securityLevel > current.securityLevel) {
            val stepUpCheck = InvariantEnforcer.assertCondition(
                ConsoleInvariant.INV_02_PROFILE_STEP_UP_INTEGRITY,
                providedPolicy.meets(requirements.minimumConfirmationPolicy),
                "Escalating from ${current.name} to ${target.name} requires confirmation policy ${requirements.minimumConfirmationPolicy.name}"
            )
            if (stepUpCheck is ConsoleResult.Failure) return stepUpCheck
        }

        if (requirements.requiresHardwareKeyStore && !hardwareKeystoreAvailable) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS7-ERR-NO-KEYSTORE",
                    message = "Profile ${target.name} requires hardware KeyStore backing, which is unavailable on this device.",
                    cnsSpec = "CNS-7"
                )
            )
        }

        if (requirements.requiresTeeOrStrongBox && !teeAvailable) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS7-ERR-NO-TEE",
                    message = "Profile ${target.name} requires hardware TEE / StrongBox, which is unavailable.",
                    cnsSpec = "CNS-7"
                )
            )
        }

        return ConsoleResult.Success(Unit)
    }

    fun selectProfile(
        target: OperationalProfile,
        hardwareKeystoreAvailable: Boolean,
        teeAvailable: Boolean,
        providedPolicy: ConfirmationPolicy
    ): ConsoleResult<OperationalProfile> {
        val validation = canSwitchTo(target, hardwareKeystoreAvailable, teeAvailable, providedPolicy)
        return when (validation) {
            is ConsoleResult.Success -> {
                _currentProfile.value = target
                ConsoleResult.Success(target)
            }
            is ConsoleResult.Failure -> ConsoleResult.Failure(validation.error)
        }
    }
}
