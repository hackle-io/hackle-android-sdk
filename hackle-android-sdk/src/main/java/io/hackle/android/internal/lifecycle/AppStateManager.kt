package io.hackle.android.internal.lifecycle

import io.hackle.sdk.core.internal.log.Logger

internal class AppStateManager : AppStateChangeListener {

    private var _currentState: AppState = AppState.BACKGROUND
    val currentState get() = _currentState

    override fun onChanged(state: AppState, timestamp: Long) {
        _currentState = state
        log.debug { "AppState changed [$state]" }
    }

    companion object {
        private val log = Logger<AppStateManager>()
    }
}
