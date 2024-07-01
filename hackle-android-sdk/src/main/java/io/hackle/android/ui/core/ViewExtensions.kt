package io.hackle.android.ui.core

import android.app.Activity
import io.hackle.sdk.core.internal.log.Logger

private val log = Logger("io.hackle.android.ViewExtensions")

internal fun Activity.setActivityRequestedOrientation(requestedOrientation: Int) {
    try {
        this.requestedOrientation = requestedOrientation
    } catch (e: Throwable) {
        log.error { "Failed to set requested orientation[$localClassName]: $e" }
    }
}
