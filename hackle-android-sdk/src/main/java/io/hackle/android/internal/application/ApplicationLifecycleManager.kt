package io.hackle.android.internal.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class ApplicationLifecycleManager(
    private val clock: Clock
) : ApplicationListenerRegistry<ApplicationLifecycleListener>(), Application.ActivityLifecycleCallbacks {

    private val enableActivities: MutableSet<Int> = mutableSetOf()
    private var isFromBackground = false

    fun registerTo(context: Context) {
        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // nothing to do
    }

    override fun onActivityStarted(activity: Activity) {
        onActivityForeground(activity.hashCode(), clock.currentMillis())
    }

    override fun onActivityResumed(activity: Activity) {
        // nothing to do
    }

    override fun onActivityPaused(activity: Activity) {
        // nothing to do
    }

    override fun onActivityStopped(activity: Activity) {
        onActivityBackground(activity.hashCode(), clock.currentMillis())
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // nothing to do
    }

    override fun onActivityDestroyed(activity: Activity) {
        // nothing to do
    }

    private fun onActivityForeground(key: Int, timestamp: Long) {
        if (enableActivities.isEmpty()) {
            log.debug { "application(onForeground)" }
            listeners.forEach { it.onApplicationForeground(timestamp, isFromBackground) }
            isFromBackground = false
        }
        enableActivities.add(key)
    }

    private fun onActivityBackground(key: Int, timestamp: Long) {
        enableActivities.remove(key)
        if (enableActivities.isEmpty() && !isFromBackground) {
            log.debug { "application(onBackground)" }
            listeners.forEach { it.onApplicationBackground(timestamp) }
            isFromBackground = true
        }
    }

    companion object {
        private val log = Logger<ApplicationLifecycleManager>()
        private var INSTANCE: ApplicationLifecycleManager? = null

        val instance: ApplicationLifecycleManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: create().also {
                        INSTANCE = it
                    }
                }
            }

        private fun create(): ApplicationLifecycleManager {
            val applicationLifecycleManager = ApplicationLifecycleManager(Clock.SYSTEM)
            applicationLifecycleManager.addListener(AppStateManager.instance, order = Ordered.LOWEST)
            return applicationLifecycleManager
        }
    }
}