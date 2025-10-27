package io.hackle.sdk.common

data class HackleWebViewConfig(
    val automaticScreenTracking: Boolean
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

        fun automaticScreenTracking(automaticScreenTracking: Boolean) = apply {
            this.automaticScreenTracking = automaticScreenTracking
        }

        fun build() = HackleWebViewConfig(automaticScreenTracking)
    }
}
