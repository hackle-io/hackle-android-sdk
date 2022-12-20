package io.hackle.android.internal.user

import io.hackle.sdk.core.user.HackleUser

internal interface UserListener {
    fun onUserUpdated(user: HackleUser, timestamp: Long)
}
