package io.hackle.android

import io.hackle.android.internal.log.AndroidLogger

class HackleConfig private constructor(builder: Builder) {

    val sdkUri: String = builder.sdkUri
    val eventUri: String = builder.eventUri

    val eventFlushIntervalMillis: Int = builder.eventFlushIntervalMillis
    val eventFlushThreshold: Int = builder.eventFlushThreshold

    val exposureEventDedupIntervalMillis: Int = builder.exposureEventDedupIntervalMillis

    class Builder {

        internal var sdkUri: String = DEFAULT_SDK_URI
        internal var eventUri: String = DEFAULT_EVENT_URI

        internal var eventFlushIntervalMillis: Int = DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS
        internal var eventFlushThreshold: Int = DEFAULT_EVENT_FLUSH_THRESHOLD

        internal var exposureEventDedupIntervalMillis: Int =
            DEFAULT_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS

        fun sdkUri(sdkUri: String) = apply {
            this.sdkUri = sdkUri
        }

        fun eventUri(eventUri: String) = apply {
            this.eventUri = eventUri
        }

        fun eventFlushIntervalMillis(eventFlushIntervalMillis: Int) = apply {
            this.eventFlushIntervalMillis = eventFlushIntervalMillis
        }

        fun eventFlushThreshold(eventFlushThreshold: Int) = apply {
            this.eventFlushThreshold = eventFlushThreshold
        }

        fun exposureEventDedupIntervalMillis(exposureEventDedupIntervalMillis: Int) = apply {
            this.exposureEventDedupIntervalMillis = exposureEventDedupIntervalMillis
        }

        fun build(): HackleConfig {

            if (exposureEventDedupIntervalMillis != EXPOSURE_EVENT_NO_DEDUP && exposureEventDedupIntervalMillis !in MIN_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS..MAX_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS) {
                log.warn { "Exposure event dedup interval is outside allowed range[${MIN_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS}..${MAX_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS}]ms. Setting to default value[$DEFAULT_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS]ms." }
                this.exposureEventDedupIntervalMillis = DEFAULT_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS
            }

            if (eventFlushIntervalMillis !in MIN_EVENT_FLUSH_INTERVAL_MILLIS..MAX_EVENT_FLUSH_INTERVAL_MILLIS) {
                log.warn { "Event flush interval is outside allowed range[${MIN_EVENT_FLUSH_INTERVAL_MILLIS}..${MAX_EVENT_FLUSH_INTERVAL_MILLIS}]ms. Setting to default value[$DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS]ms." }
                this.eventFlushIntervalMillis = DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS
            }

            if (eventFlushThreshold !in MIN_EVENT_FLUSH_THRESHOLD..MAX_EVENT_FLUSH_THRESHOLD) {
                log.warn { "Event flush threshold is outside allowed range[${MIN_EVENT_FLUSH_THRESHOLD}..${MAX_EVENT_FLUSH_THRESHOLD}]ms. Setting to default value[$DEFAULT_EVENT_FLUSH_THRESHOLD]ms." }
                this.eventFlushThreshold = DEFAULT_EVENT_FLUSH_THRESHOLD
            }

            return HackleConfig(this)
        }
    }

    companion object {

        private val log = AndroidLogger

        internal const val DEFAULT_SDK_URI = "https://client-sdk.hackle.io"
        internal const val DEFAULT_EVENT_URI = "https://event.hackle.io"

        internal const val DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS = 10 * 1000 // 10s (default)
        internal const val MIN_EVENT_FLUSH_INTERVAL_MILLIS = 1000 // 1s (min)
        internal const val MAX_EVENT_FLUSH_INTERVAL_MILLIS = 60 * 1000 // 1m (max)

        internal const val DEFAULT_EVENT_FLUSH_THRESHOLD = 10
        internal const val MIN_EVENT_FLUSH_THRESHOLD = 5
        internal const val MAX_EVENT_FLUSH_THRESHOLD = 30

        internal const val DEFAULT_EVENT_REPOSITORY_MAX_SIZE = 1000

        internal const val EXPOSURE_EVENT_NO_DEDUP = -1
        internal const val DEFAULT_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS = 60 * 1000 // 1m (default)
        internal const val MIN_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS = 1000 // 1s (min)
        internal const val MAX_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS = 60 * 60 * 1000 // 1h (max)

        val DEFAULT: HackleConfig = builder().build()

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}