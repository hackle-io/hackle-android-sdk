package io.hackle.sdk.common

/**
 * Configures session-related policies.
 *
 * Use [Builder] to create an instance. Set [persistCondition] to control
 * whether the session is preserved when user identifiers change.
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    /** Condition for preserving the session on identifier change. `null` means always start a new session. */
    val persistCondition: HackleSessionPersistCondition? = builder.persistCondition

    /** Session timeout in milliseconds. Defaults to 30 minutes. */
    val timeoutMillis: Long = builder.timeoutMillis

    override fun toString(): String = "HackleSessionPolicy(persistCondition=$persistCondition, timeoutMillis=$timeoutMillis)"

    class Builder {
        internal var persistCondition: HackleSessionPersistCondition? = null
        internal var timeoutMillis: Long = DEFAULT_SESSION_TIMEOUT_MILLIS

        /**
         * Sets the condition for preserving the session when user identifiers change.
         *
         * @param condition the condition to evaluate on identifier change
         * @return this builder instance
         */
        fun persistCondition(condition: HackleSessionPersistCondition) = apply {
            this.persistCondition = condition
        }

        /**
         * Sets the session timeout in milliseconds.
         *
         * @param timeoutMillis the session timeout in milliseconds
         * @return this builder instance
         */
        fun timeoutMillis(timeoutMillis: Long) = apply {
            this.timeoutMillis = timeoutMillis
        }

        /**
         * Builds a [HackleSessionPolicy] instance.
         *
         * @return a [HackleSessionPolicy] instance
         */
        fun build(): HackleSessionPolicy = HackleSessionPolicy(this)
    }
    
    internal fun toBuilder(): Builder = Builder().apply {
        this.persistCondition = this@HackleSessionPolicy.persistCondition
        this.timeoutMillis = this@HackleSessionPolicy.timeoutMillis
    }

    companion object {
        /**
         * Creates a new [Builder] instance for constructing a [HackleSessionPolicy].
         *
         * @return a new [Builder] instance
         */
        @JvmStatic
        fun builder(): Builder = Builder()

        internal const val DEFAULT_SESSION_TIMEOUT_MILLIS = 1000L * 60 * 30 // 30m

        /** Default policy. Always starts a new session on identifier change. */
        val DEFAULT: HackleSessionPolicy = builder().build()
    }
}
