package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface ApplicationOpenListener : ApplicationListener {
    fun onApplicationOpened(timestamp: Long)
}