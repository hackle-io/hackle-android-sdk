package io.hackle.android.internal.session

import io.hackle.sdk.common.User

internal data class SessionContext(
    val oldUser: User,
    val newUser: User,
    val timestamp: Long,
    val checkApplicationState: Boolean,
) {
    companion object {
        fun of(user: User, timestamp: Long, checkApplicationState: Boolean = false): SessionContext {
            return SessionContext(user, user, timestamp, checkApplicationState)
        }

        fun of(oldUser: User, newUser: User, timestamp: Long, checkApplicationState: Boolean = false): SessionContext {
            return SessionContext(oldUser, newUser, timestamp, checkApplicationState)
        }
    }
}
