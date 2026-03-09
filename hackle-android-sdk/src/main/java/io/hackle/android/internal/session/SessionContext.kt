package io.hackle.android.internal.session

import io.hackle.sdk.common.User

internal data class SessionContext(
    val oldUser: User,
    val newUser: User,
    val timestamp: Long,
    val isApplicationStateChange: Boolean,
) {
    companion object {
        fun of(user: User, timestamp: Long, isApplicationStateChange: Boolean = false): SessionContext {
            return SessionContext(user, user, timestamp, isApplicationStateChange)
        }

        fun of(oldUser: User, newUser: User, timestamp: Long): SessionContext {
            return SessionContext(oldUser, newUser, timestamp, isApplicationStateChange = false)
        }
    }
}
