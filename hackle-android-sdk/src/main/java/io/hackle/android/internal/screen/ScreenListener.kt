package io.hackle.android.internal.screen

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.sdk.common.Screen
import io.hackle.sdk.common.User

internal interface ScreenListener : ApplicationListener {
    fun onScreenStarted(previousScreen: Screen?, currentScreen: Screen, user: User, timestamp: Long)
    fun onScreenEnded(screen: Screen, user: User, timestamp: Long)
}
