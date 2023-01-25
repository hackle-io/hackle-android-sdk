package io.hackle.android.internal.lifecycle

internal interface AppStateChangeListener {
    fun onChanged(state: AppState, timestamp: Long)
}

internal fun <T : AppStateChangeListener> T.listen(callbacks: HackleActivityLifecycleCallbacks): T =
    apply { callbacks.addListener(this) }
