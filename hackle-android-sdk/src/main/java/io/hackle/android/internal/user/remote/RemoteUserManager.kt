package io.hackle.android.internal.user.remote

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.internal.platform.packageinfo.PackageInfo
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.recover
import io.hackle.android.internal.user.*
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

internal class RemoteUserManager(
    private val clock: Clock,
    private val device: Device,
    private val packageInfo: PackageInfo,
    private val repository: UserRepository,
    private val evaluationManager: WorkspaceEvaluationManager,
) : UserManager() {

    private val lock = Any()

    // User, Context
    private val defaultUser = User.builder().deviceId(device.id).build()
    private var context = RemoteUserContext.from(defaultUser)
    private val currentContext get() = synchronized(lock) { context }
    override val currentUser: User get() = currentContext.user

    // Initialize

    private val initSyncContext = AtomicReference<SyncContext?>()

    override fun initialize(user: User?) {
        synchronized(lock) {
            val initUser = user ?: loadUser() ?: defaultUser
            val initContext = RemoteUserContext.from(initUser.with(device))
            this.context = initContext
            this.initSyncContext.set(SyncContext(initContext, PropertyOperations.set(initUser.properties)))
            log.debug { "UserManager initialized [$initContext]" }
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
        return HackleUser.builder()
            .identifiers(user.identifiers)
            .identifier(IdentifierType.ID, user.id)
            .identifier(IdentifierType.ID, device.id, overwrite = false)
            .identifier(IdentifierType.USER, user.userId)
            .identifier(IdentifierType.DEVICE, user.deviceId)
            .identifier(IdentifierType.DEVICE, device.id, overwrite = false)
            .identifier(IdentifierType.HACKLE_DEVICE_ID, device.id)
            .properties(user.properties)
            .hackleProperties(hackleProperties(appContext, device))
            .build()
    }

    private fun hackleProperties(hackleAppContext: HackleAppContext, device: Device): Map<String, Any> {
        return hackleAppContext.browserProperties + device.properties + packageInfo.properties
    }

    // Update User

    override fun setUser(user: User): CompletableFuture<Void> {
        return updateAndSyncIfNeeded(PropertyOperations.set(user.properties)) {
            RemoteUserContext.from(user.with(device))
        }
    }

    override fun resetUser(): CompletableFuture<Void> {
        return updateAndSyncIfNeeded(PropertyOperations.clearAll()) {
            RemoteUserContext.from(defaultUser)
        }
    }

    override fun setUserId(userId: String?): CompletableFuture<Void> {
        return updateAndSyncIfNeeded { context ->
            RemoteUserContext.from(context.user.toBuilder().userId(userId).build())
        }
    }

    override fun setDeviceId(deviceId: String): CompletableFuture<Void> {
        return updateAndSyncIfNeeded { context ->
            RemoteUserContext.from(context.user.toBuilder().deviceId(deviceId).build())
        }
    }

    override fun updateProperties(operations: PropertyOperations): CompletableFuture<Void> {
        if (operations.size == 0) {
            return Futures.completed()
        }
        val context = SyncContext(currentContext, operations)
        return sync(context)
    }

    private fun updateAndSyncIfNeeded(
        operations: PropertyOperations = PropertyOperations.empty(),
        update: (RemoteUserContext) -> RemoteUserContext,
    ): CompletableFuture<Void> {
        val updated = updateContext(update)
        val syncContext = SyncContext(updated.new, operations)
        return syncIfNeeded(updated, syncContext)
    }

    private fun updateContext(update: (RemoteUserContext) -> RemoteUserContext): UserUpdated<RemoteUserContext> {
        return synchronized(lock) {
            val old = context
            val new = update(old)
            context = new

            if (!old.user.identifierEquals(new.user)) {
                userUpdated(old.user, new.user, clock.currentMillis())
            }

            UserUpdated(old, new)
        }
    }

    private fun userUpdated(old: User, new: User, timestamp: Long) {
        for (listener in listeners) {
            try {
                listener.onUserUpdated(old, new, timestamp)
            } catch (e: Exception) {
                log.error { "Failed to onUserUpdated [${listener::class.java.simpleName}]: $e" }
            }
        }
    }

    // Sync

    data class SyncContext(
        val userContext: RemoteUserContext,
        val operations: PropertyOperations,
    )

    override fun sync(): CompletableFuture<Void> {
        val context = initSyncContext.getAndSet(null) ?: SyncContext(currentContext, PropertyOperations.empty())
        return sync(context)
    }

    private fun sync(context: SyncContext): CompletableFuture<Void> {
        val hackleUser = hackleUser(context.userContext.user)
        val evaluationContext = RemoteEvaluateContext.of(hackleUser, context.operations)
        return evaluationManager.sync(evaluationContext)
            .recover { log.error { "Failed to sync WorkspaceEvaluation: $it" } }
    }

    private fun syncIfNeeded(
        updated: UserUpdated<RemoteUserContext>,
        syncContext: SyncContext
    ): CompletableFuture<Void> {
        if (syncContext.operations.size > 0) {
            return sync(syncContext)
        }

        if (updated.old.evaluationKey() != updated.new.evaluationKey()) {
            return sync(syncContext)
        }

        return Futures.completed()
    }

    // Lifecycle

    override fun onForeground(timestamp: Long, isFromBackground: Boolean) {
        // Do nothing
    }

    override fun onBackground(timestamp: Long) {
        saveUser(currentUser)
    }

    companion object {
        private val log = Logger<RemoteUserManager>()
    }
}
