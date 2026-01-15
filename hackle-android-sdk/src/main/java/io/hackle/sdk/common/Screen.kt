package io.hackle.sdk.common

import android.app.Activity

/**
 * Represents a screen in the application for tracking purposes.
 *
 * @property name the name of the screen
 * @property className the class name of the screen
 */
data class Screen internal constructor(
    val name: String,
    val className: String,
    val properties: Map<String, Any>
) {

    /**
     * Creates a Screen instance.
     */
    @Deprecated(
        message = "Use Screen.builder() instead.",
        replaceWith = ReplaceWith("Screen.builder(screenName, className).build()"),
        level = DeprecationLevel.WARNING
    )
    constructor(screenName: String, className: String) : this(
        name = screenName,
        className = className,
        properties = emptyMap()
    )

    companion object {
        /**
         * Creates a Screen instance from an Activity.
         *
         * @param activity the activity to create the screen from
         * @return a Screen instance representing the given activity
         */
        internal fun from(activity: Activity): Screen {
            val name = activity::class.java.simpleName
            return builder(name, name).build()
        }

        /**
         * Creates a Builder instance for creating Screen instances.
         * 
         * @param name the name of the screen
         * @param className the class name of the screen       
         * @return a [Builder] instance       
         */
        @JvmStatic
        fun builder(name: String, className: String): Builder {
            return Builder(name, className)
        }
    }

    /**
     * Builder class for creating Screen instances.
     */
    class Builder(
        private val name: String, 
        private val className: String
    ) {
        private var properties = PropertiesBuilder()

        /**
         * Sets a property for the screen.
         */
        fun property(key: String, value: Any?) = apply { this.properties.add(key, value) }
        
        /**
         * Sets the properties for the screen.
         */
        fun properties(properties: Map<String, Any>?) = apply {
            properties?.let { this.properties.add(it) }
        }

        /**
         * Builds a Screen instance.
         * 
         * @return a [Screen] instance
         */
        fun build(): Screen {
            return Screen(name, className, properties.build())
        }
    }
}