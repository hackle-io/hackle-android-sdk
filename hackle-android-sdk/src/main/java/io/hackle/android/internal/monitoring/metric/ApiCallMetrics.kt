package io.hackle.android.internal.monitoring.metric

import io.hackle.android.internal.http.isNotModified
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer
import okhttp3.Response

internal object ApiCallMetrics {

    inline fun record(operation: String, call: () -> Response): Response {
        val sample = Timer.start()
        return try {
            call().also { record(operation, sample, it) }
        } catch (e: Exception) {
            record(operation, sample, null)
            throw e
        }
    }

    fun record(operation: String, sample: Timer.Sample, response: Response?) {
        val tags = hashMapOf(
            "operation" to operation,
            "success" to success(response)
        )
        val timer = Metrics.timer("api.call", tags)
        sample.stop(timer)
    }

    private fun success(response: Response?): String {
        if (response == null) {
            return "false"
        }
        val success = response.isSuccessful || response.isNotModified
        return success.toString()
    }
}
