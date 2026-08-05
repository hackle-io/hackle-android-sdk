package io.hackle.android.internal.user

internal data class UserUpdated<out C : UserContext>(
    val old: C,
    val new: C
)
