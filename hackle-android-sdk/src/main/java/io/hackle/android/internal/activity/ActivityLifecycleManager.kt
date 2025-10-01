package io.hackle.android.internal.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.ui.HackleActivity
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.lang.ref.WeakReference

internal class ActivityLifecycleManager(
    private val clock: Clock
) : ApplicationListenerRegistry<ActivityLifecycleListener>(), Application.ActivityLifecycleCallbacks, ActivityProvider {

    private var state: ActivityState = ActivityState.INACTIVE
    private var activity: WeakReference<Activity>? = null
    override val currentActivity: Activity? get() = activity?.get()
    override val currentState: ActivityState get() = state

    fun registerTo(context: Context) {
        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        onLifecycle(ActivityLifecycle.CREATED, activity, clock.currentMillis())
    }

    override fun onActivityStarted(activity: Activity) {
        onLifecycle(ActivityLifecycle.STARTED, activity, clock.currentMillis())
    }

    override fun onActivityResumed(activity: Activity) {
        onLifecycle(ActivityLifecycle.RESUMED, activity, clock.currentMillis())
    }

    override fun onActivityPaused(activity: Activity) {
        onLifecycle(ActivityLifecycle.PAUSED, activity, clock.currentMillis())
    }

    override fun onActivityStopped(activity: Activity) {
        onLifecycle(ActivityLifecycle.STOPPED, activity, clock.currentMillis())
    }

    override fun onActivityDestroyed(activity: Activity) {
        onLifecycle(ActivityLifecycle.DESTROYED, activity, clock.currentMillis())
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun onLifecycle(activityLifecycle: ActivityLifecycle, activity: Activity, timestamp: Long) {
        if (activity is HackleActivity) {
            return
        }
        resolveCurrentActivity(activityLifecycle, activity)
        publish(activityLifecycle, activity, timestamp)
    }

    private fun resolveCurrentActivity(activityLifecycle: ActivityLifecycle, activity: Activity) {
        when (activityLifecycle) {
            ActivityLifecycle.CREATED, ActivityLifecycle.STARTED -> {
                setCurrentActivityIfNeeded(activity)
            }

            ActivityLifecycle.RESUMED -> {
                setCurrentActivityIfNeeded(activity)
                this.state = ActivityState.ACTIVE
            }

            ActivityLifecycle.PAUSED -> {
                this.state = ActivityState.INACTIVE
            }

            ActivityLifecycle.STOPPED -> {
                unsetCurrentActivityIfNeeded(activity)
            }

            ActivityLifecycle.DESTROYED -> Unit
        }
    }

    private fun setCurrentActivityIfNeeded(activity: Activity) {
        if (activity != currentActivity) {
            this.activity = WeakReference(activity)
        }
    }

    private fun unsetCurrentActivityIfNeeded(activity: Activity) {
        if (activity == currentActivity) {
            this.activity = null
        }
    }

    private fun publish(activityLifecycle: ActivityLifecycle, activity: Activity, timestamp: Long) {
        log.debug { "onLifecycle(lifecycle=$activityLifecycle, activity=${activity.javaClass.simpleName})" }
        for (listener in listeners) {
            try {
                listener.onLifecycle(activityLifecycle, activity, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle lifecycle [${listener.javaClass.simpleName}, $activityLifecycle]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<ActivityLifecycleManager>()
        private var INSTANCE: ActivityLifecycleManager? = null

        val instance: ActivityLifecycleManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: create().also {
                        INSTANCE = it
                    }
                }
            }

        private fun create(): ActivityLifecycleManager {
            val activityLifecycleManager = ActivityLifecycleManager(Clock.SYSTEM)
            return activityLifecycleManager
        }
    }
}
