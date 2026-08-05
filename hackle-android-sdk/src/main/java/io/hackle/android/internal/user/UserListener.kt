package io.hackle.android.internal.user

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User

internal interface UserListener : ApplicationListener {
    fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long)

    // for $properties (only Local)
    fun onPropertyOperations(user: User, operations: PropertyOperations, timestamp: Long)
}
