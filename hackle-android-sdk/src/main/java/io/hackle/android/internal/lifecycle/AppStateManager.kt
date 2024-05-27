package io.hackle.android.internal.lifecycle

import android.app.Activity
import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.Lifecycle.*
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class AppStateManager(
    private val clock: Clock,
) : ApplicationListenerRegistry<AppStateListener>(), LifecycleListener {

    private var _currentState: AppState? = null
    val currentState get() = _currentState ?: BACKGROUND

    private var executor: Executor? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun publishState() {
        val state = _currentState ?: return
        val timestamp = clock.currentMillis()
        execute {
            publish(state, timestamp)
        }
    }

    private fun onState(state: AppState, timestamp: Long) {
        log.debug { "AppState [$state]" }
        execute {
            publish(state, timestamp)
            _currentState = state
        }
    }

    private fun publish(state: AppState, timestamp: Long) {
        log.debug { "Publish AppStateEvent(state=$state)" }
        for (listener in listeners) {
            try {
                listener.onState(state, timestamp)
            } catch (e: Throwable) {
                log.error { "Failed to handle state [${listener.javaClass.simpleName}, $state]: $e" }
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

    override fun onLifecycle(lifecycle: Lifecycle, activity: Activity, timestamp: Long) {
        return when (lifecycle) {
            RESUMED -> onState(FOREGROUND, timestamp)
            PAUSED -> onState(BACKGROUND, timestamp)
            CREATED, STARTED, STOPPED, DESTROYED -> Unit
        }
    }

    companion object {
        private val log = Logger<AppStateManager>()

        private var INSTANCE: AppStateManager? = null

        val instance: AppStateManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: AppStateManager(Clock.SYSTEM).also { INSTANCE = it }
                }
            }
    }
}
