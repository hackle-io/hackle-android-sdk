package io.hackle.android.internal.engagement

import io.hackle.android.internal.screen.Screen
import io.hackle.sdk.common.User

internal data class Engagement(
    val user: User,
    val screen: Screen,
    val durationMillis: Long
)
