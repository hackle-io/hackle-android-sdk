package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.sdk.core.internal.log.Logger

internal class HackleActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private val listeners = mutableListOf<AppStateChangeListener>()

    fun addListener(listener: AppStateChangeListener) = apply {
        listeners += listener
        log.info { "AppStateChangeListener added [$listener]" }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        dispatch(FOREGROUND, System.currentTimeMillis())
    }

    override fun onActivityPaused(activity: Activity) {
        dispatch(BACKGROUND, System.currentTimeMillis())
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private fun dispatch(state: AppState, timestamp: Long) {
        for (listener in listeners) {
            try {
                listener.onChanged(state, timestamp)
            } catch (e: Exception) {
                log.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<HackleActivityLifecycleCallbacks>()
    }
}