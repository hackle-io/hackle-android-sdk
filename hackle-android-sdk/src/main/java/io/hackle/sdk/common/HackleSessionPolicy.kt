package io.hackle.sdk.common

/**
 * Configuration for session persistence policy.
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    val persistPolicy: Set<HackleSessionPersistPolicy> = builder.persistPolicy.toSet()

    override fun toString(): String = "HackleSessionPolicy(persistPolicy=$persistPolicy)"

    /**
     * Builder for creating [HackleSessionPolicy] instances.
     */
    class Builder {
        internal var persistPolicy: Set<HackleSessionPersistPolicy> = emptySet()

        /**
         * Sets the conditions under which the session is persisted.
         *
         * @param conditions the persisted conditions to enable
         * @return this builder instance
         */
        fun persistWhen(vararg conditions: HackleSessionPersistPolicy) = apply {
            this.persistPolicy = conditions.toSet()
        }

        /**
         * Builds the [HackleSessionPolicy] instance.
         *
         * @return the configured HackleSessionPolicy instance
         */
        fun build(): HackleSessionPolicy = HackleSessionPolicy(this)
    }

    companion object {
        /**
         * Creates a new [Builder] instance.
         *
         * @return a new Builder for creating HackleSessionPolicy
         */
        @JvmStatic
        fun builder(): Builder = Builder()

        /**
         * Default policy that expires session on all identifier changes.
         */
        val DEFAULT: HackleSessionPolicy = builder().build()
    }
}
