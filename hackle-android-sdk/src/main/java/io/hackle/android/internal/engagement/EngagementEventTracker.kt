package io.hackle.android.internal.engagement

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.core.HackleCore

internal class EngagementEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore,
) : EngagementListener {

    override fun onEngagement(engagement: Engagement, timestamp: Long) {
        val trackEvent = Event.builder(USER_ENGAGEMENT_EVENT_KEY)
            .property("\$screen_name", engagement.screen.name)
            .property("\$screen_class", engagement.screen.type)
            .property("\$engagement_time_ms", engagement.durationMillis)
            .build()
        val hackleUser = userManager.toHackleUser(engagement.user)
        core.track(trackEvent, hackleUser, timestamp)
    }

    companion object {
        private const val USER_ENGAGEMENT_EVENT_KEY = "\$user_engagement"
    }
}
