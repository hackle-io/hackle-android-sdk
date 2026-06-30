package io.hackle.android.internal.user.local

import io.hackle.android.internal.user.UserListener
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.model.toEvent

internal class PropertiesEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore
) : UserListener {

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {

    }

    override fun onPropertyOperations(user: User, operations: PropertyOperations, timestamp: Long) {
        val event = operations.toEvent()
        val hackleUser = userManager.hackleUser(user)
        core.track(event, hackleUser, timestamp)
        core.flush()
    }
}
