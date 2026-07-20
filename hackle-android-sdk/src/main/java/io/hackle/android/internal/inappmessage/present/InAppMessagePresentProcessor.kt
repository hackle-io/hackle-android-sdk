package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.onSuccessAsync
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal class InAppMessagePresentProcessor(
    private val coreExecutor: Executor,
    private val presenter: InAppMessagePresenter,
    private val recorder: InAppMessageRecorder,
) {
    fun process(request: InAppMessagePresentRequest): CompletableFuture<InAppMessagePresentResponse> {
        log.debug { "InAppMessage Present Request: $request" }
        return Futures.wrap { present(request) }
            .onSuccessAsync(coreExecutor) { response ->
                recorder.record(request, response)
                log.debug { "InAppMessage Present Response: $response" }
            }
    }

    private fun present(request: InAppMessagePresentRequest): CompletableFuture<InAppMessagePresentResponse> {
        val context = InAppMessagePresentationContext.of(request)
        return presenter.present(context)
    }

    companion object {
        private val log = Logger<InAppMessagePresentProcessor>()
    }
}
