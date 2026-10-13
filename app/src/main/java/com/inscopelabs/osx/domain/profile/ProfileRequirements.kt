package com.inscopelabs.osx.domain.profile

import com.inscopelabs.osx.domain.key.ConfirmationPolicy

/**
 * CNS-7: Profile Requirements specifications.
 */
data class ProfileRequirements(
    val profile: OperationalProfile,
    val requiresHardwareKeyStore: Boolean,
    val requiresTeeOrStrongBox: Boolean,
    val minimumConfirmationPolicy: ConfirmationPolicy,
    val maxEnvelopeTtlMs: Long,
    val enforceReplayCache: Boolean,
    val allowCleartextDebug: Boolean
) {
    companion object {
        fun forProfile(profile: OperationalProfile): ProfileRequirements {
            return when (profile) {
                OperationalProfile.BASIC -> ProfileRequirements(
                    profile = profile,
                    requiresHardwareKeyStore = false,
                    requiresTeeOrStrongBox = false,
                    minimumConfirmationPolicy = ConfirmationPolicy.NONE,
                    maxEnvelopeTtlMs = 120_000L, // 2 minutes
                    enforceReplayCache = false,
                    allowCleartextDebug = true
                )
                OperationalProfile.STANDARD -> ProfileRequirements(
                    profile = profile,
                    requiresHardwareKeyStore = true,
                    requiresTeeOrStrongBox = false,
                    minimumConfirmationPolicy = ConfirmationPolicy.USER_CONFIRMATION,
                    maxEnvelopeTtlMs = 30_000L, // 30 seconds
                    enforceReplayCache = true,
                    allowCleartextDebug = false
                )
                OperationalProfile.HIGH -> ProfileRequirements(
                    profile = profile,
                    requiresHardwareKeyStore = true,
                    requiresTeeOrStrongBox = true,
                    minimumConfirmationPolicy = ConfirmationPolicy.STEP_UP_AUTHENTICATED,
                    maxEnvelopeTtlMs = 10_000L, // 10 seconds
                    enforceReplayCache = true,
                    allowCleartextDebug = false
                )
            }
        }
    }
}
