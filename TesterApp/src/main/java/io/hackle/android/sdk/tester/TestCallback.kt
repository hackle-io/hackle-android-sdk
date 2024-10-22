package io.hackle.android.sdk.tester

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

class TestCallback : ActivityLifecycleCallbacks {
    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.i("HackleSdk2", "onCreated($p0)")
    }

    override fun onActivityStarted(p0: Activity) {
        Log.i("HackleSdk2", "onStarted($p0)")
    }

    override fun onActivityResumed(p0: Activity) {
        Log.i("HackleSdk2", "onResumed($p0)")
    }

    override fun onActivityPaused(p0: Activity) {
        Log.i("HackleSdk2", "onPaused($p0)")
    }

    override fun onActivityStopped(p0: Activity) {
        Log.i("HackleSdk2", "onStopped($p0)")
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        TODO("Not yet implemented")
    }

    override fun onActivityDestroyed(p0: Activity) {
        Log.i("HackleSdk2", "onDestroyed($p0)")
    }
}