package io.hackle.android.internal.session

import io.hackle.sdk.common.User

internal data class SessionContext(
    val oldUser: User,
    val newUser: User,
    val timestamp: Long,
) {
    companion object {
        fun of(user: User, timestamp: Long): SessionContext {
            return SessionContext(user, user, timestamp)
        }

        fun of(oldUser: User, newUser: User, timestamp: Long): SessionContext {
            return SessionContext(oldUser, newUser, timestamp)
        }
    }
}
