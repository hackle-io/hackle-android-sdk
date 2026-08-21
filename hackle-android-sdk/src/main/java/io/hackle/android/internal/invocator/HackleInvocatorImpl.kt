package io.hackle.android.internal.invocator

import io.hackle.android.internal.invocator.invocation.InvocationProcessor
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.task.onComplete
import io.hackle.sdk.common.HackleInvocationCallback
import io.hackle.sdk.common.HackleInvocator
import io.hackle.sdk.core.internal.log.Logger

internal class HackleInvocatorImpl(
    private val processor: InvocationProcessor,
) : HackleInvocator {

    override fun isInvocableString(string: String): Boolean {
        return InvocationRequest.isInvocableString(string)
    }

    override fun invoke(string: String): String {
        return response(string).toJsonString()
    }

    override fun invokeAsync(string: String, callback: HackleInvocationCallback) {
        val response = response(string)
        val json = response.toJsonString()
        val completion = response.completion
        if (completion == null) {
            respond(callback, json)
            return
        }
        completion.onComplete { respond(callback, json) }
    }

    private fun respond(callback: HackleInvocationCallback, json: String) {
        try {
            callback.onResponse(json)
        } catch (e: Throwable) {
            log.error { "Unexpected exception while responding to invocation: $e" }
        }
    }

    private fun response(string: String): InvocationResponse<Any> {
        return try {
            val request = InvocationRequest.parse(string)
            processor.process(request)
        } catch (e: Exception) {
            InvocationResponse.failed(e)
        }
    }

    companion object {
        private val log = Logger<HackleInvocatorImpl>()
    }
}
