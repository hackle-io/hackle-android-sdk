package io.hackle.android.internal.invocator.invocation

import io.hackle.sdk.core.internal.log.Logger

internal class InvocationProcessor(
    private val handlerFactory: InvocationHandlerFactory,
) {

    fun process(request: InvocationRequest): InvocationResponse<*> {
        return try {
            val handler = handlerFactory.get(request.command)
            handler.invoke(request)
        } catch (e: Exception) {
            log.error { "Failed to process Invocation: $e" }
            InvocationResponse.failed(e)
        }
    }

    companion object {
        private val log = Logger<InvocationProcessor>()
    }
}
