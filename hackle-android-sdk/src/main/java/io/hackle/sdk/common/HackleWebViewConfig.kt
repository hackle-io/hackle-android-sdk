package io.hackle.sdk.common

data class HackleWebViewConfig(
    val automaticScreenTracking: Boolean,
    val automaticEngagementTracking: Boolean
) {
    companion object {
        val DEFAULT: HackleWebViewConfig = builder().build()

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        private var automaticScreenTracking: Boolean = false
        private var automaticEngagementTracking: Boolean = false

        fun automaticScreenTracking(automaticScreenTracking: Boolean) = apply {
            this.automaticScreenTracking = automaticScreenTracking
        }
        fun automaticEngagementTracking(automaticEngagementTracking: Boolean) = apply {
            this.automaticEngagementTracking = automaticEngagementTracking
        }

        fun build() = HackleWebViewConfig(automaticScreenTracking, automaticEngagementTracking)
    }
}
