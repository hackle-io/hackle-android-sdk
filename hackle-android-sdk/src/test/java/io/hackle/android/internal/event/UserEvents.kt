package io.hackle.android.internal.event

import io.hackle.sdk.common.Event
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.every
import io.mockk.mockk

object UserEvents {

    fun track(
        eventKey: String,
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        timestamp: Long = System.currentTimeMillis()
    ): UserEvent.Track {
        return mockk {
            every { this@mockk.eventType } returns EventType.Custom(1, eventKey)
            every { this@mockk.event } returns Event.builder(eventKey).build()
            every { this@mockk.user } returns user
            every { this@mockk.timestamp } returns timestamp
        }
    }
}