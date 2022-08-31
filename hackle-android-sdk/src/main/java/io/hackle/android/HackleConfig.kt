package io.hackle.android

import io.hackle.android.internal.log.AndroidLogger

class HackleConfig private constructor(builder: Builder) {

    val sdkUri: String = builder.sdkUri
    val eventUri: String = builder.eventUri
    val exposureEventDedupIntervalMillis: Int = builder.exposureEventDedupIntervalMillis

    class Builder {

        internal var sdkUri: String = DEFAULT_SDK_URI
        internal var eventUri: String = DEFAULT_EVENT_URI
        internal var exposureEventDedupIntervalMillis: Int = EXPOSURE_EVENT_NO_DEDUP

        fun sdkUri(sdkUri: String) = apply {
            this.sdkUri = sdkUri
        }

        fun eventUri(eventUri: String) = apply {
            this.eventUri = eventUri
        }

        fun exposureEventDedupIntervalMillis(exposureEventDedupIntervalMillis: Int) = apply {
            this.exposureEventDedupIntervalMillis = exposureEventDedupIntervalMillis
        }

        fun build(): HackleConfig {
            if (exposureEventDedupIntervalMillis != EXPOSURE_EVENT_NO_DEDUP) {
                if (exposureEventDedupIntervalMillis < EXPOSURE_EVENT_DEDUP_INTERVAL_MIN_MILLIS || exposureEventDedupIntervalMillis > EXPOSURE_EVENT_DEDUP_INTERVAL_MAX_MILLIS) {
                    log.warn { "Exposure event dedup interval is outside allowed range[${EXPOSURE_EVENT_DEDUP_INTERVAL_MIN_MILLIS}ms..${EXPOSURE_EVENT_DEDUP_INTERVAL_MAX_MILLIS}ms]. Setting to default value[no dedup]." }
                    this.exposureEventDedupIntervalMillis = EXPOSURE_EVENT_NO_DEDUP
                }
            }
            return HackleConfig(this)
        }
    }

    companion object {

        private val log = AndroidLogger

        private const val DEFAULT_SDK_URI = "https://sdk.hackle.io"
        private const val DEFAULT_EVENT_URI = "https://event.hackle.io"

        internal const val EXPOSURE_EVENT_NO_DEDUP = -1
        private const val EXPOSURE_EVENT_DEDUP_INTERVAL_MIN_MILLIS = 1000 // 1s
        private const val EXPOSURE_EVENT_DEDUP_INTERVAL_MAX_MILLIS = 1000 * 60 * 60 // 1h

        val DEFAULT: HackleConfig = builder().build()

        fun builder(): Builder {
            return Builder()
        }
    }
}