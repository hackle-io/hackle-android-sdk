package io.hackle.android.internal.mode.webview

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
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
}
