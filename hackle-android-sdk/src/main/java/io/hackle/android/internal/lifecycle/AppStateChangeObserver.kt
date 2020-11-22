package io.hackle.android.internal.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.hackle.sdk.core.internal.log.Logger

internal class AppStateChangeObserver : LifecycleObserver {

    private val listeners = mutableListOf<AppStateChangeListener>()

    fun addListener(listener: AppStateChangeListener) = apply {
        listeners += listener
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        dispatch(AppState.FOREGROUND)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        dispatch(AppState.BACKGROUND)
    }

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
        private val log = Logger<AppStateChangeObserver>()
    }
}