package io.hackle.sdk.common

import android.app.Activity

/**
 * Represents a screen in the application for tracking purposes.
 *
 * @property name the name of the screen
 * @property className the class name of the screen
 * @property properties additional metadata for the screen
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
        replaceWith = ReplaceWith("Screen.builder(name, className).build()"),
        level = DeprecationLevel.WARNING
    )
    constructor(name: String, className: String) : this(
        name = name,
        className = className,
        properties = emptyMap()
    )

    /**
     * Compares this screen with another object for equality.
     * Two screens are considered equal if they have the same [name] and [className].
     * The [properties] field does not affect equality.
     *
     * @param other the object to compare with this screen
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Screen) return false
        return name == other.name && className == other.className
    }

    /**
     * Returns a hash code for this screen based on [name] and [className].
     * The [properties] field does not affect the hash code.
     *
     * @return the hash code
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }

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
     * 
     * @property name the name of the screen
     * @property className the class name of the screen       
     */
    class Builder(
        private val name: String, 
        private val className: String
    ) {
        private val properties = PropertiesBuilder()

        /**
         * Sets a property for the screen.
         * 
         * @param key the property key
         * @param value the property value       
         */
        fun property(key: String, value: Any?) = apply { this.properties.add(key, value) }
        
        /**
         * Sets the properties for the screen.
         * 
         * @param properties the properties to set
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