package io.hackle.sdk.common

import android.app.Activity

data class Screen(
    val name: String,
    val className: String,
) {

    companion object {
        internal fun from(activity: Activity): Screen {
            val name = activity::class.java.simpleName
            return Screen(name, name)
        }
    }
}