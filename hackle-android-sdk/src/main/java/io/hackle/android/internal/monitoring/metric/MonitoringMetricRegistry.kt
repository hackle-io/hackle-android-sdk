package io.hackle.android.internal.monitoring.metric

import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Counter
import io.hackle.sdk.core.internal.metrics.Metric
import io.hackle.sdk.core.internal.metrics.MetricRegistry
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.internal.metrics.flush.FlushCounter
import io.hackle.sdk.core.internal.metrics.flush.FlushMetric
import io.hackle.sdk.core.internal.metrics.flush.FlushTimer
import io.hackle.sdk.core.internal.time.Clock
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.Executor

internal class MonitoringMetricRegistry(
    monitoringBaseUrl: String,
    private val eventExecutor: Executor,
    private val httpExecutor: Executor,
    private val httpClient: OkHttpClient,
    clock: Clock = Clock.SYSTEM,
) : MetricRegistry(clock), AppStateListener {

    private val monitoringEndpoint = HttpUrl.get("$monitoringBaseUrl/metrics")

    override fun createCounter(id: Metric.Id): Counter {
        return FlushCounter(id)
    }

    override fun createTimer(id: Metric.Id): Timer {
        return FlushTimer(id, clock)
    }

    override fun onState(state: AppState, timestamp: Long, isFromBackground: Boolean) {
        return when (state) {
            AppState.FOREGROUND -> Unit
            AppState.BACKGROUND -> eventExecutor.execute { flush() }
        }
    }

    private fun flush() {
        httpExecutor.execute {
            try {
                metrics.asSequence()
                    .filterIsInstance<FlushMetric<Metric>>()
                    .map { it.flush() }
                    .filter(::isDispatchTarget)
                    .chunked(500)
                    .forEach(::dispatch)
            } catch (e: Exception) {
                log.warn { "Failed to flushing metrics: $e" }
            }
        }
    }

    // Dispatch only measured metrics
    private fun isDispatchTarget(metric: Metric): Boolean {
        val count = when (metric.id.type) {
            Metric.Type.COUNTER -> (metric as? Counter)?.count() ?: return false
            Metric.Type.TIMER -> (metric as? Timer)?.count() ?: return false
        }
        return count > 0
    }

    private fun dispatch(metrics: List<Metric>) {

        val batch = MetricDto.Batch(metrics.map { metric(it) })

        val requestBody = RequestBody.create(CONTENT_TYPE, batch.toJson())
        val request = Request.Builder()
            .url(monitoringEndpoint)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.warn { "Failed to flushing metrics [${response.code()}]" }
            }
        }
    }

    private fun metric(metric: Metric): MetricDto {
        return MetricDto(
            name = metric.id.name,
            tags = metric.id.tags,
            type = metric.id.type.name,
            measurements = metric.measure().associate { it.field.tagKey to it.value }
        )
    }

    override fun toString(): String {
        return "MonitoringMetricRegistry"
    }

    companion object {
        private val log = Logger<MonitoringMetricRegistry>()
        private val CONTENT_TYPE = MediaType.get("application/json; charset=utf-8")
    }
}

data class MetricDto(
    val name: String,
    val tags: Map<String, String>,
    val type: String,
    val measurements: Map<String, Double>,
) {
    data class Batch(
        val metrics: List<MetricDto>,
    )
}
