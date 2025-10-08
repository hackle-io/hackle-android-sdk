package io.hackle.android.internal.application.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class ApplicationLifecycleManager(
    private val clock: Clock
) : ApplicationListenerRegistry<ApplicationLifecycleListener>(), Application.ActivityLifecycleCallbacks {

    private val enableActivities: MutableSet<Int> = mutableSetOf()
    private var _currentState: ApplicationState? = null
    val currentState get() = _currentState ?: ApplicationState.BACKGROUND

    private var executor: Executor? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun publishStateIfNeeded() {
        val state = _currentState ?: return
        
        execute {
            log.debug { "application($state)" }
            val timestamp = clock.currentMillis()
            when (state) {
                ApplicationState.FOREGROUND -> listeners.forEach { it.onForeground(timestamp, false) }
                ApplicationState.BACKGROUND -> listeners.forEach { it.onBackground(timestamp) }
            }
        }
    }

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
            val isFromBackground = _currentState == ApplicationState.BACKGROUND
            execute {
                listeners.forEach { it.onForeground(timestamp, isFromBackground) }
                _currentState = ApplicationState.FOREGROUND
            }
        }
        enableActivities.add(key)
    }

    private fun onActivityBackground(key: Int, timestamp: Long) {
        enableActivities.remove(key)
        if (enableActivities.isEmpty()) {
            log.debug { "application(onBackground)" }
            execute {
                listeners.forEach { it.onBackground(timestamp) }
                _currentState = ApplicationState.BACKGROUND
            }
        }
    }

    private fun execute(block: () -> Unit) {
        val executor = executor
        if (executor != null) {
            executor.execute(block)
        } else {
            block()
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
            return applicationLifecycleManager
        }
    }
}