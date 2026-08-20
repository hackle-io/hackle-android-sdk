package io.hackle.android.internal.invocator

import io.hackle.android.internal.invocator.invocation.InvocationProcessor
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.sdk.common.HackleInvocationCallback
import io.hackle.sdk.common.HackleInvocator

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
            callback.onResponse(json)
            return
        }
        completion.whenComplete { _, _ -> callback.onResponse(json) }
    }

    private fun response(string: String): InvocationResponse<Any> {
        return try {
            val request = InvocationRequest.parse(string)
            processor.process(request)
        } catch (e: Exception) {
            InvocationResponse.failed(e)
        }
    }
}
