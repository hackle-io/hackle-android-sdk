package io.hackle.android.internal.session

import io.hackle.android.internal.event.UserEventDecorator
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.IdentifierType

internal class SessionUserEventDecorator(
    private val sessionManager: SessionManager
) : UserEventDecorator {
    override fun decorate(event: UserEvent): UserEvent {
        if (event.user.sessionId != null) {
            return event
        }

        val session = sessionManager.currentSession ?: return event

        val newUser = event.user.toBuilder()
            .identifier(IdentifierType.SESSION, session.id, overwrite = false)
            .build()
        return event.with(newUser)
    }
}
