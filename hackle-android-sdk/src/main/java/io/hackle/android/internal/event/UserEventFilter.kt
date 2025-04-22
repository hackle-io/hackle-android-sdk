package io.hackle.android.internal.event

import io.hackle.sdk.core.event.UserEvent

internal interface UserEventFilter {

    fun check(event: UserEvent): Result
    fun filter(event: UserEvent): UserEvent

    enum class Result {
        BLOCK,
        PASS;

        val isBlock: Boolean get() = this == BLOCK
    }
}
