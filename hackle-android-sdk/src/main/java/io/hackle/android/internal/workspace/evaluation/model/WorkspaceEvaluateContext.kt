package io.hackle.android.internal.workspace.evaluation.model

import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationRecord
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser

internal class WorkspaceEvaluateContext private constructor(
    val user: HackleUser,
    val operations: PropertyOperations,
) {

    val key: WorkspaceEvaluationRecord.Key get() = WorkspaceEvaluationRecord.keyOf(user)

    companion object {
        fun of(
            user: HackleUser,
            operations: PropertyOperations = PropertyOperations.empty(),
        ): WorkspaceEvaluateContext {
            return WorkspaceEvaluateContext(user, operations)
        }
    }
}


internal fun WorkspaceEvaluateContext.toDto(): WorkspaceEvaluateContextDto {
    return WorkspaceEvaluateContextDto(
        platformType = InAppMessage.PlatformType.ANDROID.name,
        user = HackleUserDto(
            identifiers = user.identifiers,
            userProperties = user.properties,
            hackleProperties = user.hackleProperties
        ),
        operations = operations.asMap().mapKeys { it.key.key },
    )
}
