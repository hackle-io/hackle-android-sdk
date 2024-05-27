package io.hackle.android.internal.screen

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ScreenListener : ApplicationListener {
    fun onScreenStarted(previousScreen: Screen?, currentScreen: Screen, timestamp: Long)
    fun onScreenEnded(screen: Screen, timestamp: Long)
}
