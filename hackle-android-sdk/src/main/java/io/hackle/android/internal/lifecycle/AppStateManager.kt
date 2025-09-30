package io.hackle.android.internal.lifecycle

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.application.ApplicationLifecycleListener
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class AppStateManager(
    private val clock: Clock,
) : ApplicationListenerRegistry<AppStateListener>(), ApplicationLifecycleListener {

    private var _currentState: AppState? = null
    val currentState get() = _currentState ?: BACKGROUND

    private var executor: Executor? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun publishStateIfNeeded() {
        val state = _currentState ?: return
        val timestamp = clock.currentMillis()
        execute {
            if (state == FOREGROUND) {
                publishForeground(timestamp, false)
            } else {
                publishBackground(timestamp)
            }
        }
    }

    private fun publishForeground(timestamp: Long, isFromBackground: Boolean) {
        execute { 
            log.debug { "onState(state=$FOREGROUND)" }
            for (listener in listeners) {
                try {
                    listener.onForeground(timestamp, isFromBackground)
                } catch (e: Throwable) {
                    log.error { "Failed to handle state [${listener.javaClass.simpleName}, $FOREGROUND]: $e" }
                }
            }
            _currentState = FOREGROUND
        }
    }
    
    private fun publishBackground(timestamp: Long) {
        execute { 
            log.debug { "onState(state=$BACKGROUND)" }
            for (listener in listeners) {
                try {
                    listener.onBackground(timestamp)
                } catch (e: Throwable) {
                    log.error { "Failed to handle state [${listener.javaClass.simpleName}, $BACKGROUND]: $e" }
                }
            }
            _currentState = BACKGROUND
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

    override fun onApplicationForeground(timestamp: Long, isFromBackground: Boolean) {
        publishForeground(timestamp, isFromBackground)
    }

    override fun onApplicationBackground(timestamp: Long) {
        publishBackground(timestamp)
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
