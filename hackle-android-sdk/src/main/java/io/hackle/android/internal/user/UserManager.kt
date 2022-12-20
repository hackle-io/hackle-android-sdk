package io.hackle.android.internal.user

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.user.HackleUser

internal class UserManager {

    private val userListeners = mutableListOf<UserListener>()
    private var currentUser: HackleUser? = null

    fun addListener(listener: UserListener) {
        userListeners.add(listener)
        log.debug { "UserListener added [${listener::class.java.simpleName}]" }
    }

    fun updateUser(user: HackleUser) {
        if (isUserChanged(nextUser = user)) {
            changeUser(user, System.currentTimeMillis())
        }
        currentUser = user
    }

    private fun isUserChanged(nextUser: HackleUser): Boolean {
        val currentUser = currentUser ?: return false
        return !currentUser.isSameUser(next = nextUser)
    }

    private fun changeUser(user: HackleUser, timestamp: Long) {
        for (listener in userListeners) {
            try {
                listener.onUserUpdated(user, timestamp)
            } catch (e: Exception) {
                log.error { "Failed to onUserUpdated [${listener::class.java.simpleName}]: $e" }
            }
        }
        log.debug { "User changed" }
    }

    companion object {
        private val log = Logger<UserManager>()
    }
}

private fun HackleUser.isSameUser(next: HackleUser): Boolean {
    return if (this.userId != null && next.userId != null) {
        this.userId == next.userId
    } else {
        this.deviceId == next.deviceId
    }
}
