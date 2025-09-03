package io.hackle.android.internal.event

import io.hackle.sdk.common.Event
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import java.util.UUID

object UserEvents {

    fun track(
        eventKey: String,
        insertId: String = UUID.randomUUID().toString(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        timestamp: Long = System.currentTimeMillis(),
    ): UserEvent.Track {
        return UserEvent.Track(
            insertId = insertId,
            timestamp = timestamp,
            user = user,
            eventType = EventType.Custom(1, eventKey),
            event = Event.builder(eventKey).build()
        )
    }
}