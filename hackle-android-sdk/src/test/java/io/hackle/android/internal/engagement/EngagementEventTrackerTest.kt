package io.hackle.android.internal.engagement

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.Screen
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.user.HackleUser
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EngagementEventTrackerTest {

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: EngagementEventTracker

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `track engagement event`() {
        // given
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()

        val engagement = Engagement(Screen("name", "class"), 42)

        // when
        sut.onEngagement(engagement, User.builder().build(), 43L)

        // then
        val event = Event.builder("\$engagement")
            .property("\$engagement_time_ms", 42L)
            .property("\$page_name", "name")
            .property("\$page_class", "class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 43L)
        }
    }

    @Test
    fun `track engagement event with screen properties`() {
        // given
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()

        val properties = mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        )
        val screen = Screen.builder("name", "class")
            .properties(properties)
            .build()
        val engagement = Engagement(screen, 42)

        // when
        sut.onEngagement(engagement, User.builder().build(), 43L)

        // then
        val event = Event.builder("\$engagement")
            .property("\$engagement_time_ms", 42L)
            .property("\$page_name", "name")
            .property("\$page_class", "class")
            .properties(properties)
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 43L)
        }
    }

    @Test
    fun `track engagement event with null screen properties`() {
        // given
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()

        val screen = Screen.builder("name", "class")
            .properties(null)
            .build()
        val engagement = Engagement(screen, 42)

        // when
        sut.onEngagement(engagement, User.builder().build(), 43L)

        // then
        val event = Event.builder("\$engagement")
            .property("\$engagement_time_ms", 42L)
            .property("\$page_name", "name")
            .property("\$page_class", "class")
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 43L)
        }
    }

    @Test
    fun `track engagement event with empty screen properties`() {
        // given
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()

        val screen = Screen.builder("name", "class")
            .properties(emptyMap())
            .build()
        val engagement = Engagement(screen, 42)

        // when
        sut.onEngagement(engagement, User.builder().build(), 43L)

        // then
        val event = Event.builder("\$engagement")
            .property("\$engagement_time_ms", 42L)
            .property("\$page_name", "name")
            .property("\$page_class", "class")
            .properties(emptyMap())
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 43L)
        }
    }
}
