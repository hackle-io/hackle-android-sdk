package io.hackle.android.internal.session

import io.hackle.android.internal.model.Device
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.event.UserEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class SessionEventTrackerTest {

    @Test
    fun `onSessionStarted`() {
        // given
        val userResolver = HackleUserResolver(Device("device_id", emptyMap()))
        val client = mockk<HackleInternalClient>(relaxed = true)
        val sut = SessionEventTracker(userResolver, client)

        // when
        val session = Session("42.ffffffff")
        val user = User.of("user_id")
        sut.onSessionStarted(session, user, 42)

        // then
        verify {
            client.track(
                event = withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$session_start"
                        get { properties } isEqualTo mapOf("sessionId" to "42.ffffffff")
                    }
                },
                user = withArg {
                    expectThat(it) {
                        get { sessionId } isEqualTo "42.ffffffff"
                    }
                },
                timestamp = 42
            )
        }
    }

    @Test
    fun `onSessionEnded`() {
        // given
        val userResolver = HackleUserResolver(Device("device_id", emptyMap()))
        val client = mockk<HackleInternalClient>(relaxed = true)
        val sut = SessionEventTracker(userResolver, client)

        // when
        val session = Session("42.ffffffff")
        val user = User.of("user_id")
        sut.onSessionEnded(session, user, 42)

        // then
        verify {
            client.track(
                event = withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$session_end"
                        get { properties } isEqualTo mapOf("sessionId" to "42.ffffffff")
                    }
                },
                user = withArg {
                    expectThat(it) {
                        get { sessionId } isEqualTo "42.ffffffff"
                    }
                },
                timestamp = 42
            )
        }
    }

    @Test
    fun `isSessionEvent`() {
        expectThat(SessionEventTracker.isSessionEvent(trackEvent("custom"))).isFalse()
        expectThat(SessionEventTracker.isSessionEvent(trackEvent("\$session_start"))).isTrue()
        expectThat(SessionEventTracker.isSessionEvent(trackEvent("\$session_end"))).isTrue()
    }

    private fun trackEvent(key: String): UserEvent {
        return mockk<UserEvent.Track> {
            every { event } returns Event.of(key)
        }
    }
}