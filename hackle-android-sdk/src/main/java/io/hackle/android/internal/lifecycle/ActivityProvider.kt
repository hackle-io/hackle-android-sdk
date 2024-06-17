package io.hackle.android.internal.lifecycle

import android.app.Activity

internal interface ActivityProvider {
    val currentState: ActivityState
    val currentActivity: Activity?
}

internal enum class ActivityState {
    ACTIVE,
    INACTIVE
}
