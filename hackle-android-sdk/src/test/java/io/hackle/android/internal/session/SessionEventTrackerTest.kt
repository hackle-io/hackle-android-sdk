package io.hackle.android.internal.session

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class SessionEventTrackerTest {

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: SessionEventTracker

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { userManager.hackleUser(any(), any()) } returns
            HackleUser.builder().identifier(IdentifierType.ID, "user_id").build()
    }

    @Test
    fun `onSessionStarted`() {
        // when
        sut.onSessionStarted(Session("42.ffffffff"), User.of("user_id"), 42)

        // then
        verify {
            core.track(
                event = withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$session_start"
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
        // when
        sut.onSessionEnded(Session("42.ffffffff"), User.of("user_id"), 42)

        // then
        verify {
            core.track(
                event = withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$session_end"
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
