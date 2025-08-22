package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContextResolver
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessagePresentProcessor(
    private val contextResolver: InAppMessagePresentationContextResolver,
    private val presenter: InAppMessagePresenter,
    private val recorder: InAppMessageRecorder,
) {
    fun process(request: InAppMessagePresentRequest): InAppMessagePresentResponse {
        log.debug { "InAppMessage Present Request: $request" }

        val response = present(request)
        recorder.record(request, response)

        log.debug { "InAppMessage Present Response: $response" }
        return response
    }

    private fun present(request: InAppMessagePresentRequest): InAppMessagePresentResponse {
        val context = contextResolver.resolve(request)
        presenter.present(context)
        return InAppMessagePresentResponse.of(request, context)
    }

    companion object {
        private val log = Logger<InAppMessagePresentProcessor>()
    }
}
