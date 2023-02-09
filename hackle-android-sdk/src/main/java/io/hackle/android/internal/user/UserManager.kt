package io.hackle.android.internal.user

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class UserManager(
    private val repository: KeyValueRepository,
) : AppStateChangeListener {

    private val userListeners = mutableListOf<UserListener>()
    private val defaultUser = User.builder().build()

    private var _currentUser: User? = null
    val currentUser: User get() = synchronized(LOCK) { _currentUser ?: defaultUser }

    fun initialize(user: User?) {
        synchronized(LOCK) {
            _currentUser = user ?: repository.getString(USER_KEY)?.parseJson<User>() ?: defaultUser
            log.debug { "UserManager initialized [$_currentUser]" }
        }
    }

    fun addListener(listener: UserListener) {
        userListeners.add(listener)
        log.debug { "UserListener added [${listener::class.java.simpleName}]" }
    }

    fun updateUser(user: User): User {
        val (oldUser, newUser) = synchronized(LOCK) {
            val userToMerge = this.currentUser
            val mergedUser = user.mergeWith(userToMerge)
            _currentUser = mergedUser
            userToMerge to mergedUser
        }

        if (!newUser.identifierEquals(oldUser)) {
            changeUser(oldUser, newUser, Clock.SYSTEM.currentMillis())
        }

        return newUser
    }

    private fun changeUser(oldUser: User, newUser: User, timestamp: Long) {
        for (listener in userListeners) {
            try {
                listener.onUserUpdated(oldUser, newUser, timestamp)
            } catch (e: Exception) {
                log.error { "Failed to onUserUpdated [${listener::class.java.simpleName}]: $e" }
            }
        }
        log.debug { "User changed" }
    }

    private fun saveUser(user: User) {
        repository.putString(USER_KEY, user.toJson())
        log.debug { "User saved [$user]" }
    }

    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> Unit
            BACKGROUND -> saveUser(currentUser)
        }
    }

    companion object {
        private val LOCK = Any()
        private const val USER_KEY = "user"
        private val log = Logger<UserManager>()
    }
}
