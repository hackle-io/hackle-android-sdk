package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListener
import io.hackle.android.internal.lifecycle.AppState

internal interface ApplicationStateListener : ApplicationListener {
    fun onInstall(timestamp: Long)
    fun onUpdate(timestamp: Long)
    fun onForeground(timestamp: Long, isAppLaunch: Boolean)
    fun onBackground(timestamp: Long)
}
