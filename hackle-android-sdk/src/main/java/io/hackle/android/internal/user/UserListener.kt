package io.hackle.android.internal.user

import io.hackle.sdk.common.User

internal interface UserListener {
    fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long)
}
