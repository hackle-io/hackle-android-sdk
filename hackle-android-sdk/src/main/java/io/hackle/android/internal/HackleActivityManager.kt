package io.hackle.android.internal

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import io.hackle.android.ui.HackleActivity

internal class HackleActivityManager : ActivityLifecycleCallbacks {

    var currentActivity: Activity? = null
        private set

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is HackleActivity) {
            currentActivity = activity
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is HackleActivity) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is HackleActivity) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}