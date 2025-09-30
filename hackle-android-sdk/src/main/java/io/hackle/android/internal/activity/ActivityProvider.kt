package io.hackle.android.internal.activity

import android.app.Activity

internal interface ActivityProvider {
    val currentState: ActivityState
    val currentActivity: Activity?
}

internal enum class ActivityState {
    ACTIVE,
    INACTIVE
}
