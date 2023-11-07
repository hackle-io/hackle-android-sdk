package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

internal class HackleActivityLifecycleCallbacks(
    private val eventExecutor: Executor,
    private val appStateManager: AppStateManager,
) : LifecycleManager.LifecycleEventListener {

    private val listeners = CopyOnWriteArrayList<AppStateChangeListener>()

    fun addListener(listener: AppStateChangeListener) = apply {
        listeners += listener
        log.info { "AppStateChangeListener added [$listener]" }
    }

    override fun onEvent(event: LifecycleManager.Event, timeInMillis: Long) {
        when (event) {
            LifecycleManager.Event.ON_RESUME -> dispatch(FOREGROUND, timeInMillis)
            LifecycleManager.Event.ON_PAUSE -> dispatch(BACKGROUND, timeInMillis)
            else -> Unit
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