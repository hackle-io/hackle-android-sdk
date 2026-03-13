package io.hackle.android.internal.optout

import io.hackle.android.internal.core.listener.ApplicationListener

internal interface OptOutListener : ApplicationListener {
    fun onOptOutChanged(previous: Boolean, current: Boolean)
}
