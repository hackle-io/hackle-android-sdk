package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.android.internal.workspace.evaluation.model.HackleUserDto
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContextDto
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType

internal class WorkspaceEvaluationContext private constructor(
    val user: HackleUser,
    val operations: PropertyOperations,
) {
    val key: Key get() = keyOf(user)

    data class Key(
        val identifiers: Identifiers,
    )

    companion object {

        private val EXCLUDED = setOf(IdentifierType.SESSION.key, IdentifierType.HACKLE_DEVICE_ID.key)

        fun of(
            user: HackleUser,
            operations: PropertyOperations = PropertyOperations.empty(),
        ): WorkspaceEvaluationContext {
            return WorkspaceEvaluationContext(user, operations)
        }

        fun keyOf(user: HackleUser): Key {
            return Key(Identifiers.from(user.identifiers.filterKeys { it !in EXCLUDED }))
        }

        fun keyOf(user: User): Key {
            return Key(Identifiers.from(user.resolvedIdentifiers.asMap().filterKeys { it !in EXCLUDED }))
        }
    }
}


internal fun WorkspaceEvaluationContext.toDto(): RemoteEvaluateContextDto {
    return RemoteEvaluateContextDto(
        user = HackleUserDto(
            identifiers = user.identifiers,
            userProperties = user.properties,
            hackleProperties = user.hackleProperties
        ),
        operations = operations.asMap().mapKeys { it.key.key }
    )
}
