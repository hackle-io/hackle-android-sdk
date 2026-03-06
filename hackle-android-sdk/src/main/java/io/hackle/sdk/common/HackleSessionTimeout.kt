package io.hackle.sdk.common

class HackleSessionTimeout private constructor(builder: Builder) {

    val millis: Long = builder.millis
    val onForeground: Boolean = builder.onForeground
    val onBackground: Boolean = builder.onBackground

    override fun toString(): String =
        "HackleSessionTimeout(millis=$millis, onForeground=$onForeground, onBackground=$onBackground)"

    class Builder {
        internal var millis: Long = DEFAULT_SESSION_TIMEOUT_MILLIS
        internal var onForeground: Boolean = true
        internal var onBackground: Boolean = true

        fun millis(millis: Long) = apply { this.millis = millis }
        fun onForeground(onForeground: Boolean) = apply { this.onForeground = onForeground }
        fun onBackground(onBackground: Boolean) = apply { this.onBackground = onBackground }
        fun build(): HackleSessionTimeout = HackleSessionTimeout(this)
    }

    fun toBuilder(): Builder = Builder().apply {
        this.millis = this@HackleSessionTimeout.millis
        this.onForeground = this@HackleSessionTimeout.onForeground
        this.onBackground = this@HackleSessionTimeout.onBackground
    }

    companion object {
        internal const val DEFAULT_SESSION_TIMEOUT_MILLIS = 1000L * 60 * 30 // 30m

        @JvmField
        val DEFAULT: HackleSessionTimeout = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
