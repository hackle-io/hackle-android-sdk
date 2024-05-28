package io.hackle.android.internal.screen

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ScreenEventTrackerTest {

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: ScreenEventTracker

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `onScreenStarted - when previousScreen is null then track screen event`() {
        // given
        every { userManager.resolve(any()) } returns mockk()
        val screen = Screen("test_screen_name", "test_screen_class")

        // when
        sut.onScreenStarted(null, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$screen_view")
            .property("\$screen_name", "test_screen_name")
            .property("\$screen_class", "test_screen_class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when currentScreen and previousScreen are different then track screen event`() {
        // given
        every { userManager.resolve(any()) } returns mockk()
        val screen = Screen("test_screen_name", "test_screen_class")
        val prevScreen = Screen("prev_screen_name", "prev_screen_class")

        // prevScreen
        sut.onScreenStarted(prevScreen, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$screen_view")
            .property("\$screen_name", "test_screen_name")
            .property("\$screen_class", "test_screen_class")
            .property("\$previous_screen_name", "prev_screen_name")
            .property("\$previous_screen_class", "prev_screen_class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenEnded`() {
        sut.onScreenEnded(mockk(), mockk(), 42)
    }
}
