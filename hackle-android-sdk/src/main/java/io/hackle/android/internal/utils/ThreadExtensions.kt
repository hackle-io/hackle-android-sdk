package io.hackle.android.internal.utils

import android.os.Handler
import android.os.Looper

internal fun runOnMainThread(action: Runnable) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(action)
}
