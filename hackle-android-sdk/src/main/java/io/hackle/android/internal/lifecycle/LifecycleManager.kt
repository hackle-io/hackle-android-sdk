package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.application.ApplicationStateManager
import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.Lifecycle.*
import io.hackle.android.ui.HackleActivity
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.lang.ref.WeakReference

internal class LifecycleManager(
    private val clock: Clock
) : ApplicationListenerRegistry<LifecycleListener>(), Application.ActivityLifecycleCallbacks, ActivityProvider {

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
        onLifecycle(CREATED, activity, clock.currentMillis())
    }

    override fun onActivityStarted(activity: Activity) {
        onLifecycle(STARTED, activity, clock.currentMillis())
    }

    override fun onActivityResumed(activity: Activity) {
        onLifecycle(RESUMED, activity, clock.currentMillis())
    }

    override fun onActivityPaused(activity: Activity) {
        onLifecycle(PAUSED, activity, clock.currentMillis())
    }

    override fun onActivityStopped(activity: Activity) {
        onLifecycle(STOPPED, activity, clock.currentMillis())
    }

    override fun onActivityDestroyed(activity: Activity) {
        onLifecycle(DESTROYED, activity, clock.currentMillis())
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        // TODO("핵클 엑티비티 어떻게 할건지")
        if (activity is HackleActivity) {
            return
        }
        resolveCurrentActivity(lifecycle, activity)
        publish(lifecycle, activity, timestamp)
    }

    private fun resolveCurrentActivity(lifecycle: Lifecycle, activity: Activity) {
        when (lifecycle) {
            CREATED, STARTED -> {
                setCurrentActivityIfNeeded(activity)
            }

            RESUMED -> {
                setCurrentActivityIfNeeded(activity)
                this.state = ActivityState.ACTIVE
            }

            PAUSED -> {
                this.state = ActivityState.INACTIVE
            }

            STOPPED -> {
                unsetCurrentActivityIfNeeded(activity)
            }

            DESTROYED -> Unit
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

    private fun publish(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        log.debug { "onLifecycle(lifecycle=$lifecycle, activity=${activity.javaClass.simpleName})" }
        for (listener in listeners) {
            try {
                listener.onLifecycle(lifecycle, activity, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle lifecycle [${listener.javaClass.simpleName}, $lifecycle]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<LifecycleManager>()
        private var INSTANCE: LifecycleManager? = null

        val instance: LifecycleManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: create().also {
                        INSTANCE = it
                    }
                }
            }

        private fun create(): LifecycleManager {
            val lifecycleManager = LifecycleManager(Clock.SYSTEM)
            lifecycleManager.addListener(ActivityStateManager.instance, order = Ordered.LOWEST)
            lifecycleManager.addListener(ApplicationStateManager.instance, order = Ordered.LOWEST)
            return lifecycleManager
        }
    }
}
