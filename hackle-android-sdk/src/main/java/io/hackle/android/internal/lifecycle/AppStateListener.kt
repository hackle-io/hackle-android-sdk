package io.hackle.android.internal.lifecycle

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface AppStateListener : ApplicationListener {
    fun onState(state: AppState, timestamp: Long)
}
