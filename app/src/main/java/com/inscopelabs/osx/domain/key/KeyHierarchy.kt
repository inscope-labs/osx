package com.inscopelabs.osx.domain.key

import com.inscopelabs.osx.domain.key.model.ConsoleKey
import com.inscopelabs.osx.domain.key.model.KeyRole
import com.inscopelabs.osx.domain.key.model.KeyStatus
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult

/**
 * CNS-2: Key Hierarchy Rules and Tree Verification.
 * Enforces parent-child authorization relations across console keys.
 */
class KeyHierarchy {
    fun validateSignerPrivilege(
        signerKey: ConsoleKey,
        targetRole: KeyRole
    ): ConsoleResult<Unit> {
        if (signerKey.status != KeyStatus.ACTIVE) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS2-ERR-KEY-INACTIVE",
                    message = "Signer key ${signerKey.alias} is ${signerKey.status.name}",
                    cnsSpec = "CNS-2"
                )
            )
        }

        // Higher or equal privilege level in hierarchy required (lower level index = higher authority)
        if (signerKey.role.level > targetRole.level) {
            return ConsoleResult.Failure(
                ConsoleError(
                    code = "CNS2-ERR-HIERARCHY-VIOLATION",
                    message = "Key role ${signerKey.role.name} has level ${signerKey.role.level}, insufficient to authorize target ${targetRole.name} (level ${targetRole.level})",
                    cnsSpec = "CNS-2"
                )
            )
        }

        return ConsoleResult.Success(Unit)
    }

    fun canDerive(parent: ConsoleKey, childRole: KeyRole): Boolean {
        return parent.status == KeyStatus.ACTIVE && parent.role.level < childRole.level
    }
}
