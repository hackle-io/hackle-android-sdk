package io.hackle.sdk.common

/**
 * Configures session-related policies.
 *
 * Use [Builder] to create an instance:
 * - [persistCondition]: Controls whether the session is preserved when user identifiers change.
 * - [timeoutMillis]: Sets the session timeout duration (default: 30 minutes, max: 24 hours).
 * - [enableExpiredOnBackground]: Controls whether session expiration occurs while the app is in the background.
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    /** Condition for preserving the session on identifier change. `null` means always start a new session. */
    val persistCondition: HackleSessionPersistCondition? = builder.persistCondition

    /** Session timeout in milliseconds. Defaults to 30 minutes. */
    val timeoutMillis: Long = builder.timeoutMillis

    /** Whether session expiration is enabled when the app is in the background. */
    val enableExpiredOnBackground: Boolean = builder.enableExpiredOnBackground

    override fun toString(): String = "HackleSessionPolicy(persistCondition=$persistCondition, timeoutMillis=$timeoutMillis, enableExpiredOnBackground=$enableExpiredOnBackground)"

    class Builder {
        internal var persistCondition: HackleSessionPersistCondition? = null
        internal var timeoutMillis: Long = DEFAULT_SESSION_TIMEOUT_MILLIS
        internal var enableExpiredOnBackground: Boolean = true

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
         * Sets whether session expiration is enabled when the app is in the background.
         *
         * @param enableExpiredOnBackground true to allow session restart on background events, false to prevent it
         * @return this builder instance
         */
        fun enableExpiredOnBackground(enableExpiredOnBackground: Boolean) = apply {
            this.enableExpiredOnBackground = enableExpiredOnBackground
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
        this.enableExpiredOnBackground = this@HackleSessionPolicy.enableExpiredOnBackground
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
