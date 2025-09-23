package io.hackle.android.internal.engagement

import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.sdk.common.Screen
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.user.UserManager
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class EngagementManagerTest {

    private lateinit var userManager: UserManager
    private lateinit var screenManager: ScreenManager
    private lateinit var listener: EngagementListener

    private lateinit var sut: EngagementManager

    @Before
    fun before() {
        userManager = mockk(relaxed = true)
        screenManager = mockk(relaxed = true)
        listener = mockk(relaxed = true)
        sut = EngagementManager(userManager, screenManager, 100)
        sut.addListener(listener)
    }

    @Test
    fun `onScreenStarted - start engagement`() {
        // when
        sut.onScreenStarted(null, mockk(), mockk(), 100)

        // then
        expectThat(sut.lastEngagementTime).isEqualTo(100L)
    }

    @Test
    fun `onScreenEnded - when last engagement time is null then do nothing`() {
        // given
        val screen = Screen("name", "class")

        // when
        sut.onScreenEnded(screen, mockk(), 42)

        // then
        verify { listener wasNot Called }
    }

    @Test
    fun `onScreenEnded - when engagement time is less than min time then do nothing`() {
        // given
        val screen = Screen("name", "class")
        sut.onScreenStarted(null, screen, mockk(), 42)

        // when
        sut.onScreenEnded(screen, mockk(), 141)

        // then
        verify { listener wasNot Called }
    }

    @Test
    fun `onScreenEnded - track engagement event`() {
        // given
        val screen = Screen("name", "class")
        sut.onScreenStarted(null, screen, mockk(), 42)

        // when
        sut.onScreenEnded(screen, mockk(), 142)

        // then
        verify(exactly = 1) {
            listener.onEngagement(
                withArg {
                    expectThat(it) {
                        get { durationMillis } isEqualTo 100
                    }
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `onLifecycle RESUME - startEngagement`() {
        sut.onLifecycle(Lifecycle.RESUMED, mockk(), 42)
        expectThat(sut.lastEngagementTime).isEqualTo(42)
    }

    @Test
    fun `onLifecycle PAUSED - currentScreen is null then do nothing`() {
        every { screenManager.currentScreen } returns null
        sut.onLifecycle(Lifecycle.PAUSED, mockk(), 42)
        verify { listener wasNot Called }
    }

    @Test
    fun `onLifecycle PAUSED - endEngagement`() {
        val s = Screen("name", "class")
        every { screenManager.currentScreen } returns s

        sut.onScreenStarted(null, s, mockk(), 42)
        sut.onLifecycle(Lifecycle.PAUSED, mockk(), 142)

        verify(exactly = 1) {
            listener.onEngagement(
                withArg {
                    expectThat(it) {
                        get { screen } isEqualTo Screen("name", "class")
                        get { durationMillis } isEqualTo 100
                    }
                },
                any(),
                142
            )
        }
    }

    @Test
    fun `multiple start without end - only last start is considered`() {
        // given
        val screen = Screen("name", "class")

        // when - multiple start engagements
        sut.onScreenStarted(null, screen, mockk(), 100)
        sut.onScreenStarted(screen, screen, mockk(), 200)
        sut.onScreenStarted(screen, screen, mockk(), 300)
        sut.onScreenEnded(screen, mockk(), 500)

        // then - engagement duration calculated from last start (300ms)
        verify(exactly = 1) {
            listener.onEngagement(
                withArg {
                    expectThat(it) {
                        get { durationMillis } isEqualTo 200
                    }
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `multiple end without start - no engagement published`() {
        // given
        val screen = Screen("name", "class")

        // when - multiple end engagements without start
        sut.onScreenEnded(screen, mockk(), 100)
        sut.onScreenEnded(screen, mockk(), 200)
        sut.onScreenEnded(screen, mockk(), 300)

        // then - no engagement published
        verify { listener wasNot Called }
    }

    @Test
    fun `start-end-end sequence - second end does nothing`() {
        // given
        val screen = Screen("name", "class")

        // when
        sut.onScreenStarted(null, screen, mockk(), 100)
        sut.onScreenEnded(screen, mockk(), 300) // first end - should publish
        sut.onScreenEnded(screen, mockk(), 400) // second end - should do nothing

        // then - only one engagement published
        verify(exactly = 1) {
            listener.onEngagement(
                withArg {
                    expectThat(it) {
                        get { durationMillis } isEqualTo 200
                    }
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `start-end-start-end sequence - two engagements published`() {
        // given
        val screen1 = Screen("screen1", "class1")
        val screen2 = Screen("screen2", "class2")

        // when
        sut.onScreenStarted(null, screen1, mockk(), 100)
        sut.onScreenEnded(screen1, mockk(), 300) // first engagement
        sut.onScreenStarted(screen1, screen2, mockk(), 400)
        sut.onScreenEnded(screen2, mockk(), 600) // second engagement

        // then - two engagements published
        verify(exactly = 2) {
            listener.onEngagement(any(), any(), any())
        }
    }

    @Test
    fun `lastEngagementTime cleared after endEngagement`() {
        // given
        val screen = Screen("name", "class")

        // when
        sut.onScreenStarted(null, screen, mockk(), 100)
        expectThat(sut.lastEngagementTime).isEqualTo(100L)

        sut.onScreenEnded(screen, mockk(), 300)

        // then
        expectThat(sut.lastEngagementTime).isEqualTo(null)
    }

    @Test
    fun `lastEngagementTime cleared after endEngagement even if not published`() {
        // given
        val screen = Screen("name", "class")

        // when - engagement too short to be published
        sut.onScreenStarted(null, screen, mockk(), 100)
        sut.onScreenEnded(screen, mockk(), 150) // duration 50ms < minimum 100ms

        // then - lastEngagementTime should still be cleared
        expectThat(sut.lastEngagementTime).isEqualTo(null)
        verify { listener wasNot Called }
    }
}
