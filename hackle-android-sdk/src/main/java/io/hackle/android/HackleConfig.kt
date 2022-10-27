package io.hackle.android

import io.hackle.android.internal.log.AndroidLogger

class HackleConfig private constructor(builder: Builder) {

    val sdkUri: String = builder.sdkUri
    val eventUri: String = builder.eventUri
    val exposureEventDedupIntervalMillis: Int = builder.exposureEventDedupIntervalMillis
    val flushIntervalMillis: Int = builder.flushIntervalMillis
    val eventDispatchSize: Int = builder.eventDispatchSize

    class Builder {

        internal var sdkUri: String = DEFAULT_SDK_URI
        internal var eventUri: String = DEFAULT_EVENT_URI
        internal var exposureEventDedupIntervalMillis: Int = DEFAULT_EXPOSURE_EVENT_NO_DEDUP
        internal var flushIntervalMillis: Int = DEFAULT_FLUSH_INTERVAL_MILLIS
        internal var eventDispatchSize: Int = DEFAULT_EVENT_DISPATCH_SIZE

        fun sdkUri(sdkUri: String) = apply {
            this.sdkUri = sdkUri
        }

        fun eventUri(eventUri: String) = apply {
            this.eventUri = eventUri
        }

        fun exposureEventDedupIntervalMillis(exposureEventDedupIntervalMillis: Int) = apply {
            this.exposureEventDedupIntervalMillis = exposureEventDedupIntervalMillis
        }

        fun flushIntervalMillis(flushIntervalMillis: Int) = apply {
            this.flushIntervalMillis = flushIntervalMillis
        }

        fun build(): HackleConfig {

            if (exposureEventDedupIntervalMillis != EXPOSURE_EVENT_NO_DEDUP && exposureEventDedupIntervalMillis !in EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS_RANGE) {
                log.warn { "Exposure event dedup interval is outside allowed range[$EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS_RANGE]ms. Setting to default value[$DEFAULT_EXPOSURE_EVENT_NO_DEDUP]ms." }
                this.exposureEventDedupIntervalMillis = DEFAULT_EXPOSURE_EVENT_NO_DEDUP
            }

            if (flushIntervalMillis !in FLUSH_INTERVAL_MILLIS_RANGE) {
                log.warn { "Flush interval is outside allowed range[$FLUSH_INTERVAL_MILLIS_RANGE]ms. Setting to default value[$DEFAULT_FLUSH_INTERVAL_MILLIS]ms." }
                this.flushIntervalMillis = DEFAULT_FLUSH_INTERVAL_MILLIS
            }

            if (eventDispatchSize !in EVENT_DISPATCH_SIZE_RANGE) {
                log.warn { "Event dispatch size is outside allowed range[$EVENT_DISPATCH_SIZE_RANGE]. Setting to default value[$DEFAULT_EVENT_DISPATCH_SIZE]." }
                this.eventDispatchSize = DEFAULT_EVENT_DISPATCH_SIZE
            }

            return HackleConfig(this)
        }
    }

    companion object {

        private val log = AndroidLogger

        private const val DEFAULT_SDK_URI = "https://sdk.hackle.io"
        private const val DEFAULT_EVENT_URI = "https://event.hackle.io"

        private const val DEFAULT_FLUSH_INTERVAL_MILLIS = 1000  // 1s (default)
        private val FLUSH_INTERVAL_MILLIS_RANGE = 1000..60000 // 1s ~ 60s

        private const val DEFAULT_EVENT_DISPATCH_SIZE = 10
        private val EVENT_DISPATCH_SIZE_RANGE = 5..100 // 5 ~ 100

        internal const val EXPOSURE_EVENT_NO_DEDUP = -1
        private const val DEFAULT_EXPOSURE_EVENT_NO_DEDUP = 60000 // 1m
        private val EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS_RANGE = 1000..3_600_000 // 1s ~ 1h

        val DEFAULT: HackleConfig = builder().build()

        fun builder(): Builder {
            return Builder()
        }
    }
}