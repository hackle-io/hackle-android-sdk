package io.hackle.android.internal.engagement

import io.hackle.android.internal.screen.Screen

internal data class Engagement(
    val screen: Screen,
    val durationMillis: Long
)
