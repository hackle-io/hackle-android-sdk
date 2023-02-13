package io.hackle.android.internal.user

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class UserManager(
    device: Device,
    private val repository: KeyValueRepository,
) : AppStateChangeListener {

    private val userListeners = mutableListOf<UserListener>()
    private val defaultUser = User.builder().deviceId(device.id).build()

    private var _currentUser: User = defaultUser
    val currentUser: User get() = synchronized(LOCK) { _currentUser }

    fun addListener(listener: UserListener) {
        userListeners.add(listener)
        log.debug { "UserListener added [${listener::class.java.simpleName}]" }
    }

    fun initialize(user: User?) {
        synchronized(LOCK) {
            _currentUser = user ?: loadUser() ?: defaultUser
            log.debug { "UserManager initialized [$_currentUser]" }
        }
    }

    fun setUser(user: User): User {
        return synchronized(LOCK) {
            updateUser(user)
        }
    }

    fun setUserId(userId: String?): User {
        return synchronized(LOCK) {
            val user = _currentUser.toBuilder().userId(userId).build()
            updateUser(user)
        }
    }

    fun setDeviceId(deviceId: String): User {
        return synchronized(LOCK) {
            val newUser = _currentUser.toBuilder().deviceId(deviceId).build()
            updateUser(newUser)
        }
    }

    fun setUserProperty(key: String, value: Any?): User {
        return synchronized(LOCK) {
            val newUser = _currentUser.toBuilder().property(key, value).build()
            updateUser(newUser)
        }
    }

    fun resetUser(): User {
        return synchronized(LOCK) {
            updateUser(defaultUser)
        }
    }

    private fun updateUser(user: User): User {
        val oldUser = this._currentUser
        val newUser = user.mergeWith(oldUser)
        _currentUser = newUser

        if (!newUser.identifierEquals(oldUser)) {
            changeUser(oldUser, newUser, Clock.SYSTEM.currentMillis())
        }

        log.debug { "User updated [$_currentUser]" }
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

    private fun loadUser(): User? {
        return repository.getString(USER_KEY)?.parseJson<UserModel>()?.toUser().also {
            log.debug { "User loaded [$it]" }
        }
    }

    private fun saveUser(user: User) {
        repository.putString(USER_KEY, UserModel.from(user).toJson()).also {
            log.debug { "User saved [$user]" }
        }
    }

    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> Unit
            BACKGROUND -> saveUser(currentUser)
        }
    }

    private data class UserModel(
        val id: String?,
        val userId: String?,
        val deviceId: String?,
        val identifiers: Map<String, String>,
        val properties: Map<String, Any?>,
    ) {

        fun toUser(): User {
            return User.builder()
                .id(id)
                .userId(userId)
                .deviceId(deviceId)
                .identifiers(identifiers)
                .properties(properties)
                .build()
        }

        companion object {
            fun from(user: User): UserModel {
                return UserModel(
                    id = user.id,
                    userId = user.userId,
                    deviceId = user.deviceId,
                    identifiers = user.identifiers,
                    properties = user.properties
                )
            }
        }
    }

    companion object {
        private val LOCK = Any()
        private const val USER_KEY = "user"
        private val log = Logger<UserManager>()
    }
}
