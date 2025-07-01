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
}
