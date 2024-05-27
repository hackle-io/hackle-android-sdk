package io.hackle.android.internal.engagement

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface EngagementListener : ApplicationListener {
    fun onEngagement(engagement: Engagement, timestamp: Long)
}
