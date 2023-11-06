package io.hackle.android.internal.lifecycle

import android.app.Activity

internal interface ActivityProvider {
    val currentActivity: Activity?
}