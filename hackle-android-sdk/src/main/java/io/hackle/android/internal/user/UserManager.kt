package io.hackle.android.internal.user

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.properties.operate
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.PropertyOperations
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

    fun resolve(user: User?): User {
        return if (user != null) {
            setUser(user)
        } else {
            currentUser
        }
    }

    fun setUser(user: User): User {
        return synchronized(LOCK) {
            updateUser(user)
        }
    }

    fun resetUser(): User {
        return synchronized(LOCK) {
            update { defaultUser }
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
            val user = _currentUser.toBuilder().deviceId(deviceId).build()
            updateUser(user)
        }
    }

    fun updateProperties(operations: PropertyOperations): User {
        return synchronized(LOCK) {
            operateProperties(operations)
        }
    }

    private fun updateUser(user: User): User {
        return update { currentUser ->
            user.mergeWith(currentUser)
        }
    }

    private fun operateProperties(operations: PropertyOperations): User {
        return update { currentUser ->
            val properties = operations.operate(currentUser.properties)
            currentUser.copy(properties = properties)
        }
    }

    private fun update(updater: (User) -> User): User {
        val oldUser = this._currentUser
        val newUser = updater(oldUser)
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
        return try {
            repository.getString(USER_KEY)?.parseJson<UserModel>()?.toUser().also {
                log.debug { "User loaded [$it]" }
            }
        } catch (e: Exception) {
            log.error { "Unexpected exception while load user: $e" }
            null
        }
    }

    private fun saveUser(user: User) {
        try {
            repository.putString(USER_KEY, UserModel.from(user).toJson()).also {
                log.debug { "User saved [$user]" }
            }
        } catch (e: Exception) {
            log.error { "Unexpected exception while save user: $e" }
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
        val properties: Map<String, Any>,
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
