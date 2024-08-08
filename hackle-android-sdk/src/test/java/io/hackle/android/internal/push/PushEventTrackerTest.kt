package io.hackle.android.internal.push

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.push.token.PushToken
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class PushEventTrackerTest {

    private lateinit var userManager: UserManager
    private lateinit var core: HackleCore

    private lateinit var sut: PushEventTracker

    @Before
    fun before() {
        userManager = mockk()
        core = mockk(relaxed = true)
        sut = PushEventTracker(userManager, core)
    }

    @Test
    fun `track token`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val user = User.builder().deviceId("device_id").build()
        val token = PushToken.of("token_42")

        // when
        sut.trackToken(token, user, 42)

        // then
        verify(exactly = 1) {
            core.track(
                event = withArg {
                    expectThat(it) isEqualTo Event.builder("\$push_token")
                        .property("provider_type", "FCM")
                        .property("token", "token_42")
                        .build()
                },
                user = any(),
                timestamp = 42
            )
        }
    }

    @Test
    fun `isPushTokenEvent`() {
        expectThat(PushEventTracker.isPushTokenEvent(UserEvents.track("\$push_token"))).isTrue()
        expectThat(PushEventTracker.isPushTokenEvent(UserEvents.track("\$push_click"))).isFalse()
        expectThat(PushEventTracker.isPushTokenEvent(UserEvents.track("custom"))).isFalse()
    }

    @Test
    fun `isPushClickEvent`() {
        expectThat(PushEventTracker.isPushClickEvent(UserEvents.track("\$push_token"))).isFalse()
        expectThat(PushEventTracker.isPushClickEvent(UserEvents.track("\$push_click"))).isTrue()
        expectThat(PushEventTracker.isPushClickEvent(UserEvents.track("custom"))).isFalse()
    }

    @Test
    fun `isPushEvent`() {
        expectThat(PushEventTracker.isPushEvent(UserEvents.track("\$push_token"))).isTrue()
        expectThat(PushEventTracker.isPushEvent(UserEvents.track("\$push_click"))).isTrue()
        expectThat(PushEventTracker.isPushEvent(UserEvents.track("custom"))).isFalse()
    }
}
