package io.hackle.sdk.common

/**
 * Configuration class for WebView bridge settings.
 */
data class HackleWebViewConfig(
    /**
     * Whether automatic screen tracking is enabled for WebView.
     */
    val automaticScreenTracking: Boolean,
    /**
     * Whether automatic engagement tracking is enabled for WebView.
     */
    val automaticEngagementTracking: Boolean
) {
    companion object {
        val DEFAULT: HackleWebViewConfig = builder().build()

        /**
         * Creates a new Builder instance.
         *
         * @return a [Builder] instance
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    /**
     * Builder class for creating HackleWebViewConfig instances.
     */
    class Builder {
        private var automaticScreenTracking: Boolean = false
        private var automaticEngagementTracking: Boolean = false

        /**
         * Sets whether automatic screen tracking is enabled.
         *
         * @param automaticScreenTracking true to enable automatic screen tracking, false otherwise
         * @return this Builder instance
         */
        fun automaticScreenTracking(automaticScreenTracking: Boolean) = apply {
            this.automaticScreenTracking = automaticScreenTracking
        }

        /**
         * Sets whether automatic engagement tracking is enabled.
         *
         * @param automaticEngagementTracking true to enable automatic engagement tracking, false otherwise
         * @return this Builder instance
         */
        fun automaticEngagementTracking(automaticEngagementTracking: Boolean) = apply {
            this.automaticEngagementTracking = automaticEngagementTracking
        }

        /**
         * Builds a HackleWebViewConfig instance.
         *
         * @return a [HackleWebViewConfig] instance
         */
        fun build() = HackleWebViewConfig(automaticScreenTracking, automaticEngagementTracking)
    }
}
