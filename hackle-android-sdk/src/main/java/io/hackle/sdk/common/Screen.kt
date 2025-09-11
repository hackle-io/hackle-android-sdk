package io.hackle.sdk.common

import android.app.Activity

/**
 * Represents a screen in the application for tracking purposes.
 *
 * @property name the name of the screen
 * @property className the class name of the screen
 */
data class Screen(
    val name: String,
    val className: String,
) {

    companion object {
        /**
         * Creates a Screen instance from an Activity.
         *
         * @param activity the activity to create the screen from
         * @return a Screen instance representing the given activity
         */
        internal fun from(activity: Activity): Screen {
            val name = activity::class.java.simpleName
            return Screen(name, name)
        }
    }
}