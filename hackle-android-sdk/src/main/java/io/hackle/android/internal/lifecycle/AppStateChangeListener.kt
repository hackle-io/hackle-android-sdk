package io.hackle.android.internal.lifecycle

internal interface AppStateChangeListener {
    fun onChanged(state: AppState)
}