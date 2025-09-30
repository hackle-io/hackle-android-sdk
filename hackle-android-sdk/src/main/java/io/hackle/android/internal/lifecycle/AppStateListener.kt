package io.hackle.android.internal.lifecycle

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface AppStateListener : ApplicationListener {
    fun onForeground(timestamp: Long, isFromBackground: Boolean) {}
    fun onBackground(timestamp: Long) {}
}
