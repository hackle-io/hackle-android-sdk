package io.hackle.android.internal.event

import io.hackle.sdk.common.Event
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.every
import io.mockk.mockk

object UserEvents {

    fun track(eventKey: String): UserEvent.Track {
        return mockk {
            every { eventType } returns EventType.Custom(1, eventKey)
            every { event } returns Event.builder(eventKey).build()
            every { user } returns HackleUser.builder().identifier(IdentifierType.ID, "user").build()
        }
    }
}