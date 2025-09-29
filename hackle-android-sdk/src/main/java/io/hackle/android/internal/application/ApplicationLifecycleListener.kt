package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ApplicationLifecycleListener : ApplicationListener {
    fun onApplicationForeground(timestamp: Long, isAppLaunch: Boolean)
    fun onApplicationBackground(timestamp: Long)
}