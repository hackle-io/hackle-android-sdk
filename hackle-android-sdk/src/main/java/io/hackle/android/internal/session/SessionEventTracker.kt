package io.hackle.android.internal.session

import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.user.IdentifierType

internal class SessionEventTracker(
    private val hackleUserResolver: HackleUserResolver,
    private val core: HackleCore,
) : SessionListener {

    override fun onSessionStarted(session: Session, user: User, timestamp: Long) {
        track(SESSION_START_EVENT_NAME, session, user, timestamp)
    }

    override fun onSessionEnded(session: Session, user: User, timestamp: Long) {
        track(SESSION_END_EVENT_NAME, session, user, timestamp)
    }

    private fun track(eventKey: String, session: Session, user: User, timestamp: Long) {
        val event = Event.builder(eventKey)
            .property("sessionId", session.id)
            .build()

        val hackleUser = hackleUserResolver.resolve(user)
            .toBuilder()
            .identifier(IdentifierType.SESSION, session.id, overwrite = false)
            .build()

        core.track(event, hackleUser, timestamp)
        log.debug { "$eventKey event tracked [${session.id}]" }
    }

    companion object {
        private val log = Logger<SessionEventTracker>()
        private const val SESSION_START_EVENT_NAME = "\$session_start"
        private const val SESSION_END_EVENT_NAME = "\$session_end"

        fun isSessionEvent(event: UserEvent): Boolean {
            val trackEvent = event as? UserEvent.Track ?: return false
            return trackEvent.event.key == SESSION_START_EVENT_NAME || trackEvent.event.key == SESSION_END_EVENT_NAME
        }
    }
}
