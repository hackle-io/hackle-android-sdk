package io.hackle.android.internal.screen

import android.app.Activity

internal data class Screen(
    val name: String,
    val className: String, // ScreenClass
) {

    companion object {
        fun from(activity: Activity): Screen {
            val name = activity::class.java.simpleName
            return Screen(name, name)
        }

        fun from(name: String, className: String): Screen {
            return Screen(name, className)
        }
    }
}
