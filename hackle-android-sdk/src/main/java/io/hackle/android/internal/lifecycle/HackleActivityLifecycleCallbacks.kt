package io.hackle.android.internal.lifecycle

import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.LifecycleManager.*
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

internal class HackleActivityLifecycleCallbacks(
    private val eventExecutor: Executor,
    private val appStateManager: AppStateManager,
) : LifecycleStateListener {

    private val listeners = CopyOnWriteArrayList<AppStateChangeListener>()

    fun addListener(listener: AppStateChangeListener) = apply {
        listeners += listener
        log.info { "AppStateChangeListener added [$listener]" }
    }

    override fun onState(state: LifecycleState, timeInMillis: Long) {
        when (state) {
            LifecycleState.FOREGROUND -> dispatch(FOREGROUND, timeInMillis)
            LifecycleState.BACKGROUND -> dispatch(BACKGROUND, timeInMillis)
        }
    }

    private fun dispatch(state: AppState, timestamp: Long) {
        eventExecutor.execute {
            for (listener in listeners) {
                try {
                    listener.onChanged(state, timestamp)
                } catch (e: Exception) {
                    log.error { "Unexpected exception calling ${listener::class.java.simpleName}[$state]: $e" }
                }
            }
            appStateManager.onChanged(state, timestamp)
        }
    }

    companion object {
        private val log = Logger<HackleActivityLifecycleCallbacks>()
    }
}