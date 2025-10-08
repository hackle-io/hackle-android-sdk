package io.hackle.android.internal.activity.lifecycle

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ActivityLifecycleListener : ApplicationListener {
    fun onLifecycle(activityLifecycle: ActivityLifecycle, activity: Activity, timestamp: Long)
}
