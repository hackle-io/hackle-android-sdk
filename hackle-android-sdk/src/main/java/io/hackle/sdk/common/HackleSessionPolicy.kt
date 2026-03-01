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

    override fun toString(): String = "HackleSessionPolicy(persistCondition=$persistCondition)"

    class Builder {
        internal var persistCondition: HackleSessionPersistCondition? = null

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
         * Builds a [HackleSessionPolicy] instance.
         *
         * @return a [HackleSessionPolicy] instance
         */
        fun build(): HackleSessionPolicy = HackleSessionPolicy(this)
    }

    companion object {
        /**
         * Creates a new [Builder] instance for constructing a [HackleSessionPolicy].
         *
         * @return a new [Builder] instance
         */
        @JvmStatic
        fun builder(): Builder = Builder()

        /** Default policy. Always starts a new session on identifier change. */
        val DEFAULT: HackleSessionPolicy = builder().build()
    }
}
