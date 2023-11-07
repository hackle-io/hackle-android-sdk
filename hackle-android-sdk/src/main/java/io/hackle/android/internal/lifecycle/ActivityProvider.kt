package io.hackle.android.internal.lifecycle

import android.app.Activity

interface ActivityProvider {
    val currentActivity: Activity?
}