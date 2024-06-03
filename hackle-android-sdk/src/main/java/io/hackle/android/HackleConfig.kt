package io.hackle.android

import android.util.Log
import io.hackle.android.internal.log.AndroidLogger
import java.util.Collections

class HackleConfig private constructor(builder: Builder) {

    val logLevel: Int = builder.logLevel

    val sdkUri: String = builder.sdkUri
    val eventUri: String = builder.eventUri
    val monitoringUri: String = builder.monitoringUri

    val mode: HackleAppMode = builder.mode

    val automaticScreenTracking: Boolean = builder.automaticScreenTracking

    val sessionTracking: Boolean = (mode == HackleAppMode.NATIVE && builder.sessionTracking)
    val sessionTimeoutMillis: Int = builder.sessionTimeoutMillis

    val pollingIntervalMillis: Int = builder.pollingIntervalMillis

    val eventFlushIntervalMillis: Int = builder.eventFlushIntervalMillis
    val eventFlushThreshold: Int = builder.eventFlushThreshold

    val exposureEventDedupIntervalMillis: Int = builder.exposureEventDedupIntervalMillis

    private val extra: Map<String, String> = Collections.unmodifiableMap(builder.extra)

    internal operator fun get(key: String): String? = extra[key]

    class Builder {
        internal var logLevel: Int = Log.INFO

        internal var sdkUri: String = DEFAULT_SDK_URI
        internal var eventUri: String = DEFAULT_EVENT_URI
        internal var monitoringUri: String = DEFAULT_MONITORING_URI

        internal var mode: HackleAppMode = HackleAppMode.NATIVE

        internal var automaticScreenTracking: Boolean = true

        internal var sessionTracking: Boolean = true
        internal var sessionTimeoutMillis: Int = DEFAULT_SESSION_TIMEOUT_MILLIS

        internal var pollingIntervalMillis: Int = DEFAULT_POLLING_INTERVAL_MILLIS

        internal var eventFlushIntervalMillis: Int = DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS
        internal var eventFlushThreshold: Int = DEFAULT_EVENT_FLUSH_THRESHOLD

        internal var exposureEventDedupIntervalMillis: Int =
            DEFAULT_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS

        internal val extra = hashMapOf<String, String>()

        fun logLevel(logLevel: Int) = apply {
            this.logLevel = logLevel
        }

        fun sdkUri(sdkUri: String) = apply {
            this.sdkUri = sdkUri
        }

        fun eventUri(eventUri: String) = apply {
            this.eventUri = eventUri
        }

        fun monitoringUri(monitoringUri: String) = apply {
            this.monitoringUri = monitoringUri
        }

        fun mode(mode: HackleAppMode) = apply {
            this.mode = mode
        }

        fun automaticScreenTracking(automaticScreenTracking: Boolean) = apply {
            this.automaticScreenTracking = automaticScreenTracking
        }

        fun sessionTimeoutMillis(sessionTimeoutMillis: Int) = apply {
            this.sessionTimeoutMillis = sessionTimeoutMillis
        }

        fun pollingIntervalMillis(pollingIntervalMillis: Int) = apply {
            this.pollingIntervalMillis = pollingIntervalMillis
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

        fun add(key: String, value: String) = apply {
            this.extra[key] = value
        }

        fun build(): HackleConfig {

            if (pollingIntervalMillis != NO_POLLING && pollingIntervalMillis < MIN_POLLING_INTERVAL_MILLIS) {
                log.warn { "Polling interval is outside allowed value [min ${MIN_POLLING_INTERVAL_MILLIS}ms]. Setting to min value[${MIN_POLLING_INTERVAL_MILLIS}ms]" }
                this.pollingIntervalMillis = MIN_POLLING_INTERVAL_MILLIS
            }

            if (exposureEventDedupIntervalMillis != USER_EVENT_NO_DEDUP && exposureEventDedupIntervalMillis !in MIN_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS..MAX_EXPOSURE_EVENT_DEDUP_INTERVAL_MILLIS) {
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

        internal const val DEFAULT_SDK_URI = "https://sdk.hackle.io"
        internal const val DEFAULT_EVENT_URI = "https://event.hackle.io"
        internal const val DEFAULT_MONITORING_URI = "https://monitoring.hackle.io"

        internal const val DEFAULT_SESSION_TIMEOUT_MILLIS = 1000 * 60 * 30 // 30m

        internal const val NO_POLLING = -1
        internal const val DEFAULT_POLLING_INTERVAL_MILLIS = NO_POLLING
        internal const val MIN_POLLING_INTERVAL_MILLIS = 60 * 1000 // 1m

        internal const val DEFAULT_EVENT_FLUSH_INTERVAL_MILLIS = 10 * 1000 // 10s (default)
        internal const val MIN_EVENT_FLUSH_INTERVAL_MILLIS = 1000 // 1s (min)
        internal const val MAX_EVENT_FLUSH_INTERVAL_MILLIS = 60 * 1000 // 1m (max)

        internal const val DEFAULT_EVENT_FLUSH_THRESHOLD = 10
        internal const val MIN_EVENT_FLUSH_THRESHOLD = 5
        internal const val MAX_EVENT_FLUSH_THRESHOLD = 30

        internal const val DEFAULT_EVENT_REPOSITORY_MAX_SIZE = 1000

        internal const val USER_EVENT_NO_DEDUP = -1
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