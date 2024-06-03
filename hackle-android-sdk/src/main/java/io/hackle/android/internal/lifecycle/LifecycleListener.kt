package io.hackle.android.internal.lifecycle

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListener

internal interface LifecycleListener : ApplicationListener {
    fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long)
}
