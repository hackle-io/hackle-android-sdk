package io.hackle.android.internal.session

import io.hackle.android.internal.event.UserEventDecorator
import io.hackle.sdk.core.event.UserEvent

internal class SessionUserEventDecorator(
    private val userDecorator: SessionUserDecorator,
) : UserEventDecorator {
    override fun decorate(event: UserEvent): UserEvent {
        val newUser = userDecorator.decorate(event.user)
        return event.with(newUser)
    }
}
