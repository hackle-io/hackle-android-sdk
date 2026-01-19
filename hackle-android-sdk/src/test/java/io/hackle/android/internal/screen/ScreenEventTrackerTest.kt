package io.hackle.android.internal.screen

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.Screen
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
        every { userManager.toHackleUser(any()) } returns mockk()
        val screen = Screen("test_screen_name", "test_screen_class")

        // when
        sut.onScreenStarted(null, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when currentScreen and previousScreen are different then track screen event`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val screen = Screen("test_screen_name", "test_screen_class")
        val prevScreen = Screen("prev_screen_name", "prev_screen_class")

        // prevScreen
        sut.onScreenStarted(prevScreen, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .property("\$previous_page_name", "prev_screen_name")
            .property("\$previous_page_class", "prev_screen_class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenEnded`() {
        sut.onScreenEnded(mockk(), mockk(), 42)
    }

    @Test
    fun `onScreenStarted - when screen has properties then include properties in event`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val properties = mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        )
        val screen = Screen.builder("test_screen_name", "test_screen_class")
            .properties(properties)
            .build()

        // when
        sut.onScreenStarted(null, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .properties(properties)
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when screen has properties and previous screen then include all properties in event`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val properties = mapOf("screen_key" to "screen_value")
        val screen = Screen.builder("test_screen_name", "test_screen_class")
            .properties(properties)
            .build()
        val prevScreen = Screen("prev_screen_name", "prev_screen_class")

        // when
        sut.onScreenStarted(prevScreen, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .property("\$previous_page_name", "prev_screen_name")
            .property("\$previous_page_class", "prev_screen_class")
            .properties(properties)
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when screen has null properties then event has no additional properties`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val screen = Screen.builder("test_screen_name", "test_screen_class")
            .properties(null)
            .build()

        // when
        sut.onScreenStarted(null, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when screen has empty properties then event has empty properties`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val screen = Screen.builder("test_screen_name", "test_screen_class")
            .properties(emptyMap())
            .build()

        // when
        sut.onScreenStarted(null, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "test_screen_name")
            .property("\$page_class", "test_screen_class")
            .properties(emptyMap())
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }

    @Test
    fun `onScreenStarted - when screen properties contain page_name and page_class then they are overridden by screen name and className`() {
        // given
        every { userManager.toHackleUser(any()) } returns mockk()
        val properties = mapOf(
            "\$page_name" to "wrong_name_from_properties",
            "\$page_class" to "wrong_class_from_properties",
            "\$previous_page_name" to "wrong_previous_name_from_properties",
            "\$previous_page_class" to "wrong_previous_class_from_properties",
            "custom_key" to "custom_value"
        )
        val screen = Screen.builder("correct_screen_name", "correct_screen_class")
            .properties(properties)
            .build()

        val previousScreen = Screen("correct_previous_name", "correct_previous_class")

        // when
        sut.onScreenStarted(previousScreen, screen, User.of("test"), 42)

        // then
        val event = Event.builder("\$page_view")
            .property("\$page_name", "correct_screen_name")
            .property("\$page_class", "correct_screen_class")
            .property("\$previous_page_name", "correct_previous_name")
            .property("\$previous_page_class", "correct_previous_class")
            .property("custom_key", "custom_value")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 42)
        }
    }
}
