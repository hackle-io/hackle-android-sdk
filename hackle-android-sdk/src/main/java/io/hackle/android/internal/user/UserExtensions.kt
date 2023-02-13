package io.hackle.android.internal.user

import io.hackle.sdk.common.User

internal fun User.identifierEquals(other: User?): Boolean {
    if (other == null) {
        return false
    }
    return this.userId == other.userId && this.deviceId == other.deviceId
}

internal fun User.mergeWith(other: User?): User {
    return when {
        other == null -> this
        this.identifierEquals(other) -> copy(properties = other.properties + this.properties)
        else -> this
    }
}
