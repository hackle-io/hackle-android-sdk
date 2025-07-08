package io.hackle.android.internal.monitoring.metric

import io.hackle.android.internal.http.isNotModified
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.metrics.Timer
import okhttp3.Response

internal object ApiCallMetrics {

    private const val NONE_TAG = "NONE"

    inline fun record(operation: String, call: () -> Response): Response {
        val sample = Timer.start()
        return try {
            call().also { record(operation, sample, it, null) }
        } catch (e: Throwable) {
            record(operation, sample, null, e)
            throw e
        }
    }

    fun record(operation: String, sample: Timer.Sample, response: Response?, e: Throwable?) {
        val tags = hashMapOf(
            "operation" to operation,
            "success" to success(response),
            "status" to status(response),
            "exception" to exception(e),
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

    private fun status(response: Response?): String {
        return response?.code()?.toString() ?: NONE_TAG
    }

    private fun exception(e: Throwable?): String {
        if (e == null) {
            return NONE_TAG
        }
        val simpleName = e::class.java.simpleName
        return simpleName.ifBlank { e::class.java.name }
    }
}
