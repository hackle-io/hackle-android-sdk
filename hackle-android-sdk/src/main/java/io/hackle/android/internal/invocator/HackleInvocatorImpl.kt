package io.hackle.android.internal.invocator

import io.hackle.android.internal.invocator.invocation.InvocationProcessor
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.sdk.common.HackleInvocator

internal class HackleInvocatorImpl(
    private val processor: InvocationProcessor,
) : HackleInvocator {

    override fun isInvocableString(string: String): Boolean {
        return InvocationRequest.isInvocableString(string)
    }

    override fun invoke(string: String): String {
        val response = try {
            val request = InvocationRequest.parse(string)
            processor.process(request)
        } catch (e: Exception) {
            InvocationResponse.failed(e)
        }
        return response.toJsonString()
    }
}
