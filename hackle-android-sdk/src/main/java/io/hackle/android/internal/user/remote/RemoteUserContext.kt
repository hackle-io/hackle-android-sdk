package io.hackle.android.internal.user.remote

import io.hackle.android.internal.user.UserContext
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationRecord
import io.hackle.sdk.common.User

internal class RemoteUserContext private constructor(
    override val user: User, // only identifiers
) : UserContext {

    companion object {
        fun from(user: User): RemoteUserContext {
            return RemoteUserContext(
                user = sanitize(user),
            )
        }

        private fun sanitize(user: User): User {
            if (user.properties.isEmpty()) {
                return user
            }
            return user.toBuilder()
                .properties(emptyMap())
                .build()
        }
    }
}

internal fun RemoteUserContext.evaluationKey(): WorkspaceEvaluationRecord.Key {
    return WorkspaceEvaluationRecord.keyOf(user)
}
