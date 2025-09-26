package io.hackle.android.internal.application

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.android.internal.lifecycle.Lifecycle.CREATED
import io.hackle.android.internal.lifecycle.Lifecycle.DESTROYED
import io.hackle.android.internal.lifecycle.Lifecycle.PAUSED
import io.hackle.android.internal.lifecycle.Lifecycle.RESUMED
import io.hackle.android.internal.lifecycle.Lifecycle.STARTED
import io.hackle.android.internal.lifecycle.Lifecycle.STOPPED
import io.hackle.android.internal.lifecycle.LifecycleListener
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.Executor

internal class ApplicationStateManager
    : ApplicationListenerRegistry<ApplicationStateListener>(), LifecycleListener, ApplicationOpenListener {
    
    private var enableActivities: MutableSet<Int> = mutableSetOf()

    private var executor: Executor? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }
    
    private fun onActivityForeground(key: Int, timestamp: Long) {
        execute {
            if(enableActivities.isEmpty()) {
                publish(FOREGROUND, timestamp)
            }
            enableActivities.add(key)
        }
    }
    
    private fun onActivityBackground(key: Int, timestamp: Long) {
        execute {
            enableActivities.remove(key)
            if(enableActivities.isEmpty()) {
                publish(BACKGROUND, timestamp)
            }
        }
    }
    
    private fun publish(state: AppState, timestamp: Long) {
        log.debug { "application(lifecycle=$state)" }

        listeners.forEach { listener ->
            listener.onState(state, timestamp)
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

    override fun onApplicationOpened(timestamp: Long) {
        log.debug { "application(OPEN)" }
        execute {
            listeners.forEach { listener ->
                listener.onOpen(timestamp)
            }
        }
    }

    override fun onLifecycle(
        lifecycle: Lifecycle,
        activity: Activity,
        timestamp: Long
    ) {
        return when (lifecycle) {
            STARTED -> onActivityForeground(activity.hashCode(), timestamp)
            STOPPED -> onActivityBackground(activity.hashCode(), timestamp)
            CREATED, RESUMED, PAUSED, DESTROYED -> Unit
        }
    }

    companion object Companion {
        private val log = Logger<ApplicationStateManager>()

        private var INSTANCE: ApplicationStateManager? = null

        val instance: ApplicationStateManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ApplicationStateManager().also { INSTANCE = it }
                }
            }
    }
}
