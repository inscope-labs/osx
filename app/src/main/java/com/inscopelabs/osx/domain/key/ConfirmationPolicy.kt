package com.inscopelabs.osx.domain.key

/**
 * CNS-2: Confirmation Policies for key authorization and profile step-up.
 */
enum class ConfirmationPolicy(
    val level: Int,
    val label: String,
    val description: String
) {
    NONE(
        level = 0,
        label = "Direct Execution",
        description = "No explicit authorization dialog needed; automatic verification."
    ),
    USER_CONFIRMATION(
        level = 1,
        label = "Operator Confirmation",
        description = "Explicit console operator prompt required before signing."
    ),
    STEP_UP_AUTHENTICATED(
        level = 2,
        label = "Hardware Step-Up",
        description = "Cryptographic biometric or hardware key presence confirmation required."
    );

    fun meets(required: ConfirmationPolicy): Boolean {
        return this.level >= required.level
    }
}
