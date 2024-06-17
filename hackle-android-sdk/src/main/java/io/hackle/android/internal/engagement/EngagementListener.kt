package io.hackle.android.internal.engagement

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.sdk.common.User

internal interface EngagementListener : ApplicationListener {
    fun onEngagement(engagement: Engagement, user: User, timestamp: Long)
}
