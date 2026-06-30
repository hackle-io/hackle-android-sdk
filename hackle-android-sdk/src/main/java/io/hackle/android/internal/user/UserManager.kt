package io.hackle.android.internal.user

import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.context.HackleAppContext.Companion.DEFAULT
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.sync.Synchronizer
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser
import java.util.concurrent.CompletableFuture

internal abstract class UserManager :
    ApplicationListenerRegistry<UserListener>(),
    ApplicationLifecycleListener,
    Synchronizer {

    abstract val currentUser: User
    abstract fun initialize(user: User?)
    abstract fun hackleUser(user: User = currentUser, appContext: HackleAppContext = DEFAULT): HackleUser

    abstract fun setUser(user: User): CompletableFuture<Void>
    abstract fun resetUser(): CompletableFuture<Void>
    abstract fun setUserId(userId: String?): CompletableFuture<Void>
    abstract fun setDeviceId(deviceId: String): CompletableFuture<Void>
    abstract fun updateProperties(operations: PropertyOperations): CompletableFuture<Void>
}
