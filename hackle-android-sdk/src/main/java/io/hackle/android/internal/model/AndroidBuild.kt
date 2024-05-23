package io.hackle.android.internal.model

import android.os.Build

internal object AndroidBuild {
    fun sdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}
