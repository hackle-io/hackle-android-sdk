package io.hackle.android.internal.user

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.properties.operate
import io.hackle.android.internal.sync.Synchronizer
import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import java.util.concurrent.CopyOnWriteArrayList


internal class UserManager(
    private val device: Device,
    private val repository: KeyValueRepository,
    private val cohortFetcher: UserCohortFetcher,
) : Synchronizer, AppStateChangeListener {

    private val userListeners = CopyOnWriteArrayList<UserListener>()
    private val defaultUser = User.builder().deviceId(device.id).build()
    private var context: UserContext = UserContext.of(defaultUser, UserCohorts.empty())
    private val currentContext: UserContext get() = synchronized(LOCK) { context }
    val currentUser: User get() = currentContext.user

    fun addListener(listener: UserListener) {
        userListeners.add(listener)
        log.debug { "UserListener added [${listener::class.java.simpleName}]" }
    }

    fun initialize(user: User?) {
        synchronized(LOCK) {
            val initUser = user ?: loadUser() ?: defaultUser
            context = UserContext.of(initUser, UserCohorts.empty())
            log.debug { "UserManager initialized [$context]" }
        }
    }

    fun resolve(user: User?): HackleUser {
        val context = user?.let { setUser(user) } ?: currentContext
        return toHackleUser(context)
    }

    fun toHackleUser(user: User): HackleUser {
        val context = currentContext.with(user)
        return toHackleUser(context)
    }

    private fun toHackleUser(context: UserContext): HackleUser {
        return HackleUser.builder()
            .identifiers(context.user.identifiers)
            .identifier(IdentifierType.ID, context.user.id)
            .identifier(IdentifierType.ID, device.id, overwrite = false)
            .identifier(IdentifierType.USER, context.user.userId)
            .identifier(IdentifierType.DEVICE, context.user.deviceId)
            .identifier(IdentifierType.DEVICE, device.id, overwrite = false)
            .identifier(IdentifierType.HACKLE_DEVICE_ID, device.id)
            .properties(context.user.properties)
            .hackleProperties(device.properties)
            .cohorts(context.cohorts.rawCohorts())
            .build()
    }

    override fun sync() {
        val cohorts = cohortFetcher.fetch(currentUser)
        synchronized(LOCK) {
            context = context.update(cohorts)
        }
    }

    fun setUser(user: User): UserContext {
        return synchronized(LOCK) {
            updateUser(user)
        }
    }

    fun resetUser(): UserContext {
        return synchronized(LOCK) {
            update { defaultUser }
        }
    }

    fun setUserId(userId: String?): UserContext {
        return synchronized(LOCK) {
            val user = context.user.toBuilder().userId(userId).build()
            updateUser(user)
        }
    }

    fun setDeviceId(deviceId: String): UserContext {
        return synchronized(LOCK) {
            val user = context.user.toBuilder().deviceId(deviceId).build()
            updateUser(user)
        }
    }

    fun updateProperties(operations: PropertyOperations): UserContext {
        return synchronized(LOCK) {
            operateProperties(operations)
        }
    }

    private fun updateUser(user: User): UserContext {
        return update { currentUser ->
            user.with(device).mergeWith(currentUser)
        }
    }

    private fun operateProperties(operations: PropertyOperations): UserContext {
        return update { currentUser ->
            val properties = operations.operate(currentUser.properties)
            currentUser.copy(properties = properties)
        }
    }

    private fun update(updater: (User) -> User): UserContext {
        val oldUser = this.context.user
        val newUser = updater(oldUser)

        val newContext = context.with(newUser)
        context = newContext

        if (!newUser.identifierEquals(oldUser)) {
            changeUser(oldUser, newUser, Clock.SYSTEM.currentMillis())
        }

        log.debug { "User updated [$newContext]" }
        return newContext
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
