package io.hackle.android.internal.engagement

import io.hackle.sdk.common.Screen

internal data class Engagement(
    val screen: Screen,
    val durationMillis: Long
)
