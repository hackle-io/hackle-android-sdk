package io.hackle.android.internal.mode.webview

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.EventType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class WebViewWrapperUserEventFilterTest {

    @Test
    fun `when not push event then return BLOCK`() {
        val sut = WebViewWrapperUserEventFilter()
        val actual = sut.check(UserEvents.track("test"))
        expectThat(actual).isEqualTo(UserEventFilter.Result.BLOCK)
    }

    @Test
    fun `when deviceId = hackleDeviceId then BLOCK`() {
        val sut = WebViewWrapperUserEventFilter()
        val user = HackleUser.builder()
            .identifier(IdentifierType.DEVICE, "device")
            .identifier(IdentifierType.HACKLE_DEVICE_ID, "device")
            .build()
        val actual = sut.check(UserEvents.track("\$push_token", user))
        expectThat(actual).isEqualTo(UserEventFilter.Result.BLOCK)
    }

    @Test
    fun `pass`() {
        val sut = WebViewWrapperUserEventFilter()
        val user = HackleUser.builder()
            .identifier(IdentifierType.DEVICE, "device")
            .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device")
            .build()
        val actual = sut.check(UserEvents.track("\$push_token", user))
        expectThat(actual).isEqualTo(UserEventFilter.Result.PASS)
    }

    @Test
    fun `filter clears user properties`() {
        val sut = WebViewWrapperUserEventFilter()
        val user = HackleUser.builder()
            .identifier(IdentifierType.DEVICE, "device")
            .hackleProperty("key1", "value1")
            .properties(mapOf("key2" to "value2"))
            .build()


        val track = UserEvents.track("\$push_token", user)

        every {
            track.with(any())
        } returns UserEvents.track(
            "\$push_token", user
                .toBuilder()
                .clearProperties()
                .build()
        )

        val filteredEvent = sut.filter(track)

        expectThat(filteredEvent.user.properties).isEqualTo(emptyMap())
    }
}
