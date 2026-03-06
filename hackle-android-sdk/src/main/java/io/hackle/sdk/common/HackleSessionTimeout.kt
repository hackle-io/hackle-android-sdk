package io.hackle.sdk.common

class HackleSessionTimeout private constructor(builder: Builder) {

    val millis: Long = builder.millis
    val enableOnForeground: Boolean = builder.enableOnForeground
    val enableOnBackground: Boolean = builder.enableOnBackground

    override fun toString(): String =
        "HackleSessionTimeout(millis=$millis, enableOnForeground=$enableOnForeground, enableOnBackground=$enableOnBackground)"

    class Builder {
        internal var millis: Long = DEFAULT_SESSION_TIMEOUT_MILLIS
        internal var enableOnForeground: Boolean = false
        internal var enableOnBackground: Boolean = true

        fun millis(millis: Long) = apply { this.millis = millis }
        fun enableOnForeground(enableOnForeground: Boolean) = apply { this.enableOnForeground = enableOnForeground }
        fun enableOnBackground(enableOnBackground: Boolean) = apply { this.enableOnBackground = enableOnBackground }
        fun build(): HackleSessionTimeout = HackleSessionTimeout(this)
    }

    fun toBuilder(): Builder = Builder().apply {
        this.millis = this@HackleSessionTimeout.millis
        this.enableOnForeground = this@HackleSessionTimeout.enableOnForeground
        this.enableOnBackground = this@HackleSessionTimeout.enableOnBackground
    }

    companion object {
        internal const val DEFAULT_SESSION_TIMEOUT_MILLIS = 1000L * 60 * 30 // 30m

        @JvmField
        val DEFAULT: HackleSessionTimeout = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
