package com.inscopelabs.osx.domain.session

import com.inscopelabs.osx.domain.foundation.ConsoleInvariant
import com.inscopelabs.osx.domain.foundation.InvariantEnforcer
import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.key.ConsoleKeyManager
import com.inscopelabs.osx.domain.key.model.KeyRole
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.profile.ProfileRequirements
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.session.model.SessionState
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import com.inscopelabs.osx.util.generateId

/**
 * CNS-4: Session Start Protocol.
 * Validates requirements, enforces confirmation policies, and provisions session instance.
 */
class SessionStart(
    private val keyManager: ConsoleKeyManager
) {
    fun initiateSession(
        operatorId: String,
        targetControllerId: String,
        profile: OperationalProfile,
        operatorConfirmationPolicy: ConfirmationPolicy
    ): ConsoleResult<ConsoleSession> {
        val requirements = ProfileRequirements.forProfile(profile)

        // Enforce confirmation requirements
        if (!operatorConfirmationPolicy.meets(requirements.minimumConfirmationPolicy)) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS4-ERR-POLICY-DEFICIT",
                    message = "Session initiation under ${profile.name} requires confirmation policy ${requirements.minimumConfirmationPolicy.name}, but received ${operatorConfirmationPolicy.name}",
                    cnsSpec = "CNS-4"
                )
            )
        }

        // Check active signing key
        val signingKey = keyManager.getActiveKey(KeyRole.SIGNING)
            ?: return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS4-ERR-NO-SIGNER",
                    message = "No active signing key available in key hierarchy to anchor session.",
                    cnsSpec = "CNS-4"
                )
            )

        val sessionId = generateId("sess")
        val session = ConsoleSession(
            sessionId = sessionId,
            operatorId = operatorId,
            targetControllerId = targetControllerId,
            profile = profile,
            state = SessionState.ACTIVE,
            sessionKeyId = signingKey.id
        )

        return ConsoleResult.Success(session)
    }
}
