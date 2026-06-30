package io.hackle.android.internal.user

import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.sync.Synchronizer
import io.hackle.android.internal.task.Task
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser

internal abstract class UserManager :
    ApplicationListenerRegistry<UserListener>(),
    ApplicationLifecycleListener,
    Synchronizer {

    abstract val currentUser: User
    abstract fun initialize(user: User?)
    abstract fun hackleUser(user: User = currentUser, appContext: HackleAppContext = HackleAppContext.DEFAULT): HackleUser

    abstract fun setUser(user: User): Task<Unit>
    abstract fun resetUser(): Task<Unit>
    abstract fun setUserId(userId: String?): Task<Unit>
    abstract fun setDeviceId(deviceId: String): Task<Unit>
    abstract fun updateProperties(operations: PropertyOperations): Task<Unit>
}
