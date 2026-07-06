package io.hackle.android.internal.workspace.evaluation.model

import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.core.model.PlatformType
import io.hackle.sdk.core.user.HackleUser

internal class WorkspaceEvaluateContext private constructor(
    val platformType: PlatformType,
    val user: HackleUser,
    val operations: PropertyOperations,
) {

    val key: WorkspaceEvaluationContext.Key get() = WorkspaceEvaluationContext.keyOf(user)

    companion object {
        fun of(
            user: HackleUser,
            operations: PropertyOperations = PropertyOperations.empty(),
        ): WorkspaceEvaluateContext {
            return WorkspaceEvaluateContext(
                platformType = PlatformType.ANDROID,
                user = user,
                operations = operations
            )
        }
    }
}


internal fun WorkspaceEvaluateContext.toDto(): WorkspaceEvaluateContextDto {
    return WorkspaceEvaluateContextDto(
        platformType = platformType.name,
        user = HackleUserDto(
            identifiers = user.identifiers,
            userProperties = user.properties,
            hackleProperties = user.hackleProperties
        ),
        operations = operations.asMap().mapKeys { it.key.key },
    )
}
