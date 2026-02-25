package io.hackle.sdk.common

/**
 * Configuration for session expiration policy.
 *
 * Controls which identifier changes trigger session renewal.
 * By default, all identifier changes cause session expiration.
 *
 * Can only be set at SDK initialization time via [io.hackle.android.HackleConfig].
 */
class HackleSessionPolicy private constructor(builder: Builder) {

    /**
     * The set of conditions that trigger session expiration.
     * If any of the conditions match, the session will be renewed.
     */
    val expiredPolicy: Set<HackleSessionExpiry> = builder.expiredPolicy.toSet()

    /**
     * Builder for creating [HackleSessionPolicy] instances.
     */
    class Builder {
        internal var expiredPolicy: Set<HackleSessionExpiry> = HackleSessionExpiry.values().toSet()

        /**
         * Sets the conditions that trigger session expiration.
         *
         * @param conditions the session expiry conditions to enable
         * @return this builder instance
         */
        fun expiredPolicy(vararg conditions: HackleSessionExpiry) = apply {
            this.expiredPolicy = conditions.toSet()
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
