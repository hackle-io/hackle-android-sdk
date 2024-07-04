package io.hackle.android.internal.user

import io.hackle.android.internal.core.Updated
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.core.map
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.properties.operate
import io.hackle.android.internal.sync.Synchronizer
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType


internal class UserManager(
    private val device: Device,
    private val repository: KeyValueRepository,
    private val cohortFetcher: UserCohortFetcher,
) : ApplicationListenerRegistry<UserListener>(), Synchronizer, AppStateListener {

    private val defaultUser = User.builder().deviceId(device.id).build()
    private var context: UserContext = UserContext.of(defaultUser, UserCohorts.empty())
    private val currentContext: UserContext get() = synchronized(LOCK) { context }
    val currentUser: User get() = currentContext.user

    fun initialize(user: User?) {
        synchronized(LOCK) {
            val initUser = user ?: loadUser() ?: defaultUser
            context = UserContext.of(initUser.with(device), UserCohorts.empty())
            log.debug { "UserManager initialized [$context]" }
        }
    }


    // HackleUser resolve

    fun resolve(user: User?): HackleUser {
        if (user == null) {
            return toHackleUser(currentContext)
        }
        val context = synchronized(LOCK) {
            updateUser(user)
        }
        return toHackleUser(context.current)
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

    // Sync

    override fun sync() {
        val cohorts = try {
            cohortFetcher.fetch(currentUser)
        } catch (e: Exception) {
            log.error { "Failed to fetch cohorts: $e" }
            return
        }
        synchronized(LOCK) {
            context = context.update(cohorts)
        }
    }

    fun syncIfNeeded(updated: Updated<User>) {
        if (hasNewIdentifiers(updated.previous, updated.current)) {
            sync()
        }
    }

    private fun hasNewIdentifiers(previousUser: User, currentUser: User): Boolean {
        val previousIdentifiers = previousUser.resolvedIdentifiers
        val currentIdentifiers = currentUser.resolvedIdentifiers.asList()
        return currentIdentifiers.any { it !in previousIdentifiers }
    }

    // User Update

    fun setUser(user: User): Updated<User> {
        return synchronized(LOCK) {
            updateUser(user).map { it.user }
        }
    }

    fun resetUser(): Updated<User> {
        return synchronized(LOCK) {
            updateContext { defaultUser }.map { it.user }
        }
    }

    fun setUserId(userId: String?): Updated<User> {
        return synchronized(LOCK) {
            val user = context.user.toBuilder().userId(userId).build()
            updateUser(user).map { it.user }
        }
    }

    fun setDeviceId(deviceId: String): Updated<User> {
        return synchronized(LOCK) {
            val user = context.user.toBuilder().deviceId(deviceId).build()
            updateUser(user).map { it.user }
        }
    }

    fun updateProperties(operations: PropertyOperations): Updated<User> {
        return synchronized(LOCK) {
            operateProperties(operations).map { it.user }
        }
    }

    private fun updateUser(user: User): Updated<UserContext> {
        return updateContext { currentUser ->
            user.with(device).mergeWith(currentUser)
        }
    }

    private fun operateProperties(operations: PropertyOperations): Updated<UserContext> {
        return updateContext { currentUser ->
            val properties = operations.operate(currentUser.properties)
            currentUser.copy(properties = properties)
        }
    }

    private fun updateContext(updater: (User) -> User): Updated<UserContext> {
        val oldContext = this.context
        val oldUser = oldContext.user
        val newUser = updater(oldUser)

        val newContext = context.with(newUser)
        context = newContext

        if (!newUser.identifierEquals(oldUser)) {
            changeUser(oldUser, newUser, Clock.SYSTEM.currentMillis())
        }

        log.debug { "User updated [${newContext.user}]" }
        return Updated(oldContext, newContext)
    }

    private fun changeUser(oldUser: User, newUser: User, timestamp: Long) {
        log.debug { "onUserUpdated(oldUser=$oldUser, newUser=$newUser)" }
        for (listener in listeners) {
            try {
                listener.onUserUpdated(oldUser, newUser, timestamp)
            } catch (e: Exception) {
                log.error { "Failed to onUserUpdated [${listener::class.java.simpleName}]: $e" }
            }
        }
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

    override fun onState(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> Unit
            BACKGROUND -> saveUser(currentUser)
        }
    }

    data class UserModel(
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
