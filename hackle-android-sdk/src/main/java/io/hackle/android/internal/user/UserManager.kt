package io.hackle.android.internal.user

import io.hackle.android.internal.application.ApplicationLifecycleListener
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.core.Updated
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.core.map
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.platform.packageinfo.PackageInfo
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
    private val packageInfo: PackageInfo,
    private val repository: KeyValueRepository,
    private val cohortFetcher: UserCohortFetcher,
    private val targetEventFetcher: UserTargetEventFetcher,
) : ApplicationListenerRegistry<UserListener>(), Synchronizer, ApplicationLifecycleListener {

    private val defaultUser = User.builder().deviceId(device.id).build()
    private var context: UserContext = UserContext.of(defaultUser, UserCohorts.empty(), UserTargetEvents.empty())
    private val currentContext: UserContext get() = synchronized(LOCK) { context }
    val currentUser: User get() = currentContext.user

    fun initialize(user: User?) {
        synchronized(LOCK) {
            val initUser = user ?: loadUser() ?: defaultUser
            context = UserContext.of(initUser.with(device), UserCohorts.empty(), UserTargetEvents.empty())
            log.debug { "UserManager initialized [$context]" }
        }
    }


    // HackleUser resolve

    fun resolve(user: User?, hackleAppContext: HackleAppContext): HackleUser {
        if (user == null) {
            return toHackleUser(currentContext, hackleAppContext)
        }
        val context = synchronized(LOCK) {
            updateUser(user)
        }
        return toHackleUser(context.current, hackleAppContext)
    }

    fun toHackleUser(user: User): HackleUser {
        val context = currentContext.with(user)
        return toHackleUser(context, HackleAppContext.DEFAULT)
    }

    private fun toHackleUser(context: UserContext, hackleAppContext: HackleAppContext): HackleUser {
        return HackleUser.builder()
            .identifiers(context.user.identifiers)
            .identifier(IdentifierType.ID, context.user.id)
            .identifier(IdentifierType.ID, device.id, overwrite = false)
            .identifier(IdentifierType.USER, context.user.userId)
            .identifier(IdentifierType.DEVICE, context.user.deviceId)
            .identifier(IdentifierType.DEVICE, device.id, overwrite = false)
            .identifier(IdentifierType.HACKLE_DEVICE_ID, device.id)
            .properties(context.user.properties)
            .hackleProperties(hackleProperties(hackleAppContext, device))
            .cohorts(context.cohorts.rawCohorts())
            .targetEvents(context.targetEvents.rawEvents())
            .build()
    }
    
    private fun hackleProperties(hackleAppContext: HackleAppContext, device: Device): Map<String, Any> {
        return hackleAppContext.browserProperties + device.properties + packageInfo.properties
    }

    // Sync

    override fun sync() {
        syncCohort()
        syncTargetEvents()
    }

    /**
     * 사용자 정보가 변경되었을 때 필요한 경우 동기화를 수행한다
     *
     * cohort 는 사용자 새로운 식별자가 설정되었을 때만 동기화한다.
     * target event 는 userId or deviceId 가 변경되었을 때 동기화한다.
     * @param updated 변경된 사용자 정보
     */
    fun syncIfNeeded(updated: Updated<User>) {
        if (hasNewIdentifiers(updated.previous, updated.current)) {
            syncCohort()
        }

        if (!updated.previous.identifierEquals(updated.current)) {
            syncTargetEvents()
        }
    }

    /**
     * cohort 정보를 동기화한다.
     */
    private fun syncCohort() {
        val cohort = fetchCohort() ?: return
        synchronized(LOCK) {
            context = context.update(cohort)
        }
    }

    /**
     * target event 정보를 동기화한다.
     */
    private fun syncTargetEvents() {
        val targetEvents = fetchTargetEvent() ?: return
        synchronized(LOCK) {
            context = context.update(targetEvents)
        }
    }

    /**
     * cohort 정보를 조회한다.
     * @return cohorts
     */
    private fun fetchCohort(): UserCohorts? {
        return try {
            cohortFetcher.fetch(currentUser)
        } catch (e: Exception) {
            log.error { "Failed to fetch cohort: $e" }
            return null
        }
    }

    /**
     * target event 정보를 조회한다.
     * @return targetEvents
     */
    private fun fetchTargetEvent(): UserTargetEvents? {
        return try {
            targetEventFetcher.fetch(currentUser)
        } catch (e: Exception) {
            log.error { "Failed to fetch userTargetEvent: $e" }
            return null
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

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        // nothing to do
    }

    override fun onBackground(timestamp: Long) {
        saveUser(currentUser)
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
