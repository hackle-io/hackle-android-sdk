package io.hackle.android.internal.event

import io.hackle.sdk.core.event.UserEvent

internal interface EventListener {
    fun onEventPublish(userEvent: UserEvent)
}