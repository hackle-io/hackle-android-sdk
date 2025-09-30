package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ApplicationInstallStateListener : ApplicationListener {
    fun onInstall(timestamp: Long)
    fun onUpdate(timestamp: Long)
}
