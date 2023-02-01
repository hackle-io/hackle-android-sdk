package io.hackle.android.internal.monitoring.metric

import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer
import okhttp3.Response

internal object ApiCallMetrics {

    inline fun record(operation: String, call: () -> Response): Response {
        val sample = Timer.start()
        return try {
            call().also { record(operation, sample, it.isSuccessful) }
        } catch (e: Exception) {
            record(operation, sample, false)
            throw e
        }
    }
    fun record(operation: String, sample: Timer.Sample, isSuccess: Boolean) {
        val tags = hashMapOf(
            "operation" to operation,
            "success" to isSuccess.toString()
        )
        val timer = Metrics.timer("api.call", tags)
        sample.stop(timer)
    }
}
