package io.hackle.android.internal.application.lifecycle

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ApplicationLifecycleListener : ApplicationListener {
    fun onForeground(timestamp: Long, isFromBackground: Boolean)
    fun onBackground(timestamp: Long)
}
