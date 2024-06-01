package io.hackle.android.internal.engagement

import io.hackle.android.internal.screen.ScreenEventTracker.Companion.SCREEN_CLASS_PROPERTY_KEY
import io.hackle.android.internal.screen.ScreenEventTracker.Companion.SCREEN_NAME_PROPERTY_KEY
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore

internal class EngagementEventTracker(
    private val userManager: UserManager,
    private val core: HackleCore,
) : EngagementListener {

    override fun onEngagement(engagement: Engagement, user: User, timestamp: Long) {
        val trackEvent = Event.builder(ENGAGEMENT_EVENT_KEY)
            .property(ENGAGEMENT_TIME_PROPERTY_KEY, engagement.durationMillis)
            .property(SCREEN_NAME_PROPERTY_KEY, engagement.screen.name)
            .property(SCREEN_CLASS_PROPERTY_KEY, engagement.screen.className)
            .build()
        val hackleUser = userManager.toHackleUser(user)
        core.track(trackEvent, hackleUser, timestamp)
    }

    companion object {
        const val ENGAGEMENT_EVENT_KEY = "\$engagement"
        const val ENGAGEMENT_TIME_PROPERTY_KEY = "\$engagement_time_ms"
    }
}
