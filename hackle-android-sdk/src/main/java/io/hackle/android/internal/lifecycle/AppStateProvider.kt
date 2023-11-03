package io.hackle.android.internal.lifecycle

internal interface AppStateProvider {

    val currentState: AppState?
}