package io.hackle.android.internal

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import io.hackle.android.HackleActivity

internal class HackleActivityManager : ActivityLifecycleCallbacks {

    var currentActivity: Activity? = null
        private set

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = if (activity is HackleActivity) {
            return
        } else activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = if (activity is HackleActivity) {
            return
        } else activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = if (activity is HackleActivity) {
            return
        } else activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}