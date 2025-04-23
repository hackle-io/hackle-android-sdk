package io.hackle.android.internal.event

import io.hackle.sdk.core.event.UserEvent

internal interface UserEventDecorator {
    fun decorate(event: UserEvent): UserEvent
}
