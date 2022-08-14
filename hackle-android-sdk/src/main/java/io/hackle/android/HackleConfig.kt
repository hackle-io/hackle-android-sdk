package io.hackle.android

import io.hackle.android.internal.log.AndroidLogger

class HackleConfig private constructor(builder: Builder) {

    val sdkUri: String = builder.sdkUri
    val eventUri: String = builder.eventUri
    val exposureEventDedupIntervalMillis: Long? = builder.exposureEventDedupIntervalMillis

    class Builder {

        internal var sdkUri: String = DEFAULT_SDK_URI
        internal var eventUri: String = DEFAULT_EVENT_URI
        internal var exposureEventDedupIntervalMillis: Long? = null

        fun sdkUri(sdkUri: String) = apply {
            this.sdkUri = sdkUri
        }

        fun eventUri(eventUri: String) = apply {
            this.eventUri = eventUri
        }

        fun exposureEventDedupIntervalSeconds(exposureEventDedupIntervalSeconds: Int) = apply {
            this.exposureEventDedupIntervalMillis = exposureEventDedupIntervalSeconds * 1000L
        }

        fun build(): HackleConfig {
            val dedupInterval = exposureEventDedupIntervalMillis

            if (dedupInterval != null) {
                if (dedupInterval < EXPOSURE_EVENT_DEDUP_INTERVAL_MIN_MILLIS || dedupInterval > EXPOSURE_EVENT_DEDUP_INTERVAL_MAX_MILLIS) {
                    log.warn { "Exposure event dedup interval is outside allowed range[1s..1h]. Setting to default value[no dedup]." }
                    this.exposureEventDedupIntervalMillis = null
                }
            }
            return HackleConfig(this)
        }
    }

    companion object {

        private val log = AndroidLogger

        private const val DEFAULT_SDK_URI = "https://sdk.hackle.io"
        private const val DEFAULT_EVENT_URI = "https://event.hackle.io"

        private const val EXPOSURE_EVENT_DEDUP_INTERVAL_MIN_MILLIS = 1000L // 1s
        private const val EXPOSURE_EVENT_DEDUP_INTERVAL_MAX_MILLIS = 1000L * 60L * 60L // 1h

        val DEFAULT: HackleConfig = builder().build()

        fun builder(): Builder {
            return Builder()
        }
    }
}