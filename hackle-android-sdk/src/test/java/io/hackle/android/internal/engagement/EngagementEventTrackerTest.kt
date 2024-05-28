package io.hackle.android.internal.engagement

import io.hackle.android.internal.screen.Screen
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class EngagementEventTrackerTest {
    @Test
    fun `track engagement event`() {
        // given
        val userManager = mockk<UserManager>()
        val core = mockk<HackleCore>(relaxed = true)
        val sut = EngagementEventTracker(userManager, core)

        every { userManager.toHackleUser(any()) } returns HackleUser.builder().build()

        val engagement = Engagement(Screen("name", "class"), 42)

        // when
        sut.onEngagement(engagement, User.builder().build(), 43L)

        // then
        val event = Event.builder("\$user_engagement")
            .property("\$screen_name", "name")
            .property("\$screen_class", "class")
            .property("\$engagement_time_ms", 42L)
            .build()
        verify(exactly = 1) {
            core.track(event, any(), 43L)
        }
    }
}
