package io.hackle.android.internal.user.local

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.properties.operate
import io.hackle.android.internal.task.Task
import io.hackle.android.internal.user.*
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType


internal class LocalUserManager(
    private val clock: Clock,
    private val device: Device,
    private val packageInfo: PackageInfo,
    private val repository: UserRepository,
    private val cohortFetcher: UserCohortFetcher,
    private val targetEventFetcher: UserTargetEventFetcher,
) : UserManager() {

    private val lock = Any()

    // User, Context

    private val defaultUser = User.builder().deviceId(device.id).build()
    private var context: LocalUserContext =
        LocalUserContext.of(defaultUser, UserCohorts.empty(), UserTargetEvents.empty())
    private val currentContext: LocalUserContext get() = synchronized(lock) { context }
    override val currentUser: User get() = currentContext.user

    // Initialize

    override fun initialize(user: User?) {
        synchronized(lock) {
            val initUser = user ?: loadUser() ?: defaultUser
            context = LocalUserContext.of(initUser.with(device), UserCohorts.empty(), UserTargetEvents.empty())
            log.debug { "UserManager initialized [$context]" }
        }
    }

    private fun loadUser(): User? {
        return try {
            repository.get()
        } catch (e: Exception) {
            log.error { "Failed to load User: $e" }
            null
        }
    }

    private fun saveUser(user: User) {
        try {
            repository.set(user)
        } catch (e: Exception) {
            log.error { "Failed to save User: $e" }
        }
    }

    // HackleUser resolve

    override fun hackleUser(user: User, appContext: HackleAppContext): HackleUser {
        val context = currentContext.with(user)
        return HackleUser.builder()
            .identifiers(context.user.identifiers)
            .identifier(IdentifierType.ID, context.user.id)
            .identifier(IdentifierType.ID, device.id, overwrite = false)
            .identifier(IdentifierType.USER, context.user.userId)
            .identifier(IdentifierType.DEVICE, context.user.deviceId)
            .identifier(IdentifierType.DEVICE, device.id, overwrite = false)
            .identifier(IdentifierType.HACKLE_DEVICE_ID, device.id)
            .properties(context.user.properties)
            .hackleProperties(hackleProperties(appContext, device))
            .cohorts(context.cohorts.rawCohorts())
            .targetEvents(context.targetEvents.rawEvents())
            .build()
    }

    private fun hackleProperties(hackleAppContext: HackleAppContext, device: Device): Map<String, Any> {
        return hackleAppContext.browserProperties + device.properties + packageInfo.properties
    }

    // User Update

    override fun setUser(user: User): Task<Unit> {
        return update { context ->
            context.with(user.with(device).mergeWith(context.user))
        }
    }

    override fun resetUser(): Task<Unit> {
        val updated = updateContext { context ->
            context.with(defaultUser)
        }
        trackProperties(updated.new.user, PropertyOperations.clearAll(), clock.currentMillis())
        return syncIfNeeded(updated)
    }

    override fun setUserId(userId: String?): Task<Unit> {
        return update { context ->
            val user = context.user.toBuilder().userId(userId).build()
            context.with(user.with(device).mergeWith(context.user))
        }
    }

    override fun setDeviceId(deviceId: String): Task<Unit> {
        return update { context ->
            val user = context.user.toBuilder().deviceId(deviceId).build()
            context.with(user.with(device).mergeWith(context.user))
        }
    }

    override fun updateProperties(operations: PropertyOperations): Task<Unit> {
        return update { context ->
            trackProperties(context.user, operations, clock.currentMillis())
            val user = context.user.copy(properties = operations.operate(context.user.properties))
            context.with(user)
        }
    }

    private fun trackProperties(user: User, operations: PropertyOperations, timestamp: Long) {
        for (listener in listeners) {
            listener.onPropertyOperations(user, operations, timestamp)
        }
    }

    private fun update(update: (LocalUserContext) -> LocalUserContext): Task<Unit> {
        val updated = updateContext(update)
        return syncIfNeeded(updated)
    }

    private fun updateContext(update: (LocalUserContext) -> LocalUserContext): UserUpdated<LocalUserContext> {
        return synchronized(lock) {
            val oldContext = this.context
            val newContext = update(oldContext)
            this.context = newContext

            if (!newContext.user.identifierEquals(oldContext.user)) {
                changeUser(oldContext.user, newContext.user, clock.currentMillis())
            }

            log.debug { "User updated [${newContext.user}]" }
            UserUpdated(oldContext, newContext)
        }
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

    // Sync

    private fun syncIfNeeded(updated: UserUpdated<LocalUserContext>): Task<Unit> {
        val tasks = mutableListOf<Task<Unit>>()
        if (hasNewIdentifiers(updated.old.user, updated.new.user)) {
            tasks.add(syncCohort())
        }
        if (!updated.old.user.identifierEquals(updated.new.user)) {
            tasks.add(syncTargetEvents())
        }
        return Task.all(tasks).then()
    }

    /**
     * cohort 정보를 동기화한다.
     */
    private fun syncCohort(): Task<Unit> {
        return cohortFetcher.fetch(currentUser)
            .map {
                synchronized(lock) {
                    context = context.update(it)
                }
            }
            .recover {
                log.error { "Failed to fetch cohort: $it" }
            }
    }

    /**
     * target event 정보를 동기화한다.
     */
    private fun syncTargetEvents(): Task<Unit> {
        return targetEventFetcher.fetch(currentUser)
            .map {
                synchronized(lock) {
                    context = context.update(it)
                }
            }
            .recover {
                log.error { "Failed to fetch target events: $it" }
            }
    }

    /**
     * 사용자 식별자가 변경되었는지 확인한다.
     * @return 변경되었으면 true, 아니면 false
     */
    private fun hasNewIdentifiers(previousUser: User, currentUser: User): Boolean {
        val previousIdentifiers = previousUser.resolvedIdentifiers
        val currentIdentifiers = currentUser.resolvedIdentifiers.asList()
        return currentIdentifiers.any { it !in previousIdentifiers }
    }

    override fun sync(): Task<Unit> {
        return Task.all(syncCohort(), syncTargetEvents()).then()
    }

    // Lifecycle

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        // nothing to do
    }

    override fun onBackground(timestamp: Long) {
        saveUser(currentUser)
    }

    companion object {
        private val log = Logger<LocalUserManager>()
    }
}
