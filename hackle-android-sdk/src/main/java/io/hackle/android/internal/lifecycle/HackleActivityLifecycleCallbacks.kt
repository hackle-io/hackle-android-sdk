package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger

internal class HackleActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private val listeners = mutableListOf<AppStateChangeListener>()

    fun addListener(listener: AppStateChangeListener) = apply {
        listeners += listener
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        dispatch(AppState.FOREGROUND)
    }

    override fun onActivityPaused(activity: Activity) {
        dispatch(AppState.BACKGROUND)
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private fun dispatch(state: AppState) {
        for (listener in listeners) {
            try {
                listener.onChanged(state)
            } catch (e: Exception) {
                log.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<HackleActivityLifecycleCallbacks>()
    }
}