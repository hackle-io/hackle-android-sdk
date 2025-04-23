package io.hackle.android.internal.mode.webview

import io.hackle.sdk.common.Event
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewWrapperUserEventDecoratorTest {

    @Test
    fun `decorate should work on UserEvent_Track and return cleared user properties`() {
        // Arrange
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID.key, "userId")
            .property("trackingKey", "trackingValue")
            .hackleProperties(mapOf("key" to "value"))
            .build()

        val eventKey = "\$push_token"

        val trackEvent = UserEvent.Track(
            insertId = "",
            timestamp = 0L,
            user = user,
            eventType = EventType.Custom(1, eventKey),
            event = Event.builder(eventKey).build()
        )

        val decorator = WebViewWrapperUserEventDecorator()

        // Act
        val decoratedTrackEvent = decorator.decorate(trackEvent)

        // Assert
        assertEquals(0, decoratedTrackEvent.user.properties.size)
        assertEquals(trackEvent.user.id, decoratedTrackEvent.user.id)
        assertEquals(trackEvent.user.hackleProperties, decoratedTrackEvent.user.hackleProperties)
    }
}