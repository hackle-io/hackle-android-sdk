package io.hackle.sdk.common

/**
 * Configuration for session timeout behavior.
 */
class HackleSessionTimeoutCondition private constructor(builder: Builder) {

    val timeoutMillis: Long = builder.timeoutMillis
    val onForeground: Boolean = builder.onForeground
    val onBackground: Boolean = builder.onBackground
    val onApplicationStateChange: Boolean = builder.onApplicationStateChange

    override fun toString(): String =
        "HackleSessionTimeout(millis=${this@HackleSessionTimeoutCondition.timeoutMillis}, enableOnForeground=${this@HackleSessionTimeoutCondition.onForeground}, enableOnBackground=${this@HackleSessionTimeoutCondition.onBackground}, enableOnApplicationStateChange=${this@HackleSessionTimeoutCondition.onApplicationStateChange})"

    class Builder {
        internal var timeoutMillis: Long = DEFAULT_SESSION_TIMEOUT_MILLIS
        internal var onForeground: Boolean = false
        internal var onBackground: Boolean = true
        internal var onApplicationStateChange: Boolean = true

        fun millis(millis: Long) = apply { this.timeoutMillis = millis }
        fun enableOnForeground(enableOnForeground: Boolean) = apply { this.onForeground = enableOnForeground }
        fun enableOnBackground(enableOnBackground: Boolean) = apply { this.onBackground = enableOnBackground }
        fun enableOnApplicationStateChange(enableOnApplicationStateChange: Boolean) = apply { this.onApplicationStateChange = enableOnApplicationStateChange }
        fun build(): HackleSessionTimeoutCondition = HackleSessionTimeoutCondition(this)
    }

    fun toBuilder(): Builder = Builder().apply {
        this.timeoutMillis = this@HackleSessionTimeoutCondition.timeoutMillis
        this.onForeground = this@HackleSessionTimeoutCondition.onForeground
        this.onBackground = this@HackleSessionTimeoutCondition.onBackground
        this.onApplicationStateChange = this@HackleSessionTimeoutCondition.onApplicationStateChange
    }

    companion object {
        internal const val DEFAULT_SESSION_TIMEOUT_MILLIS = 1000L * 60 * 30 // 30m

        @JvmField
        val DEFAULT: HackleSessionTimeoutCondition = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
