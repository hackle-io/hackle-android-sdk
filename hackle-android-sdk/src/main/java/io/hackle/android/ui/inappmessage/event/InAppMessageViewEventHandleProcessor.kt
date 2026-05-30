package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageViewEventHandleProcessor(
    private val handlerFactory: InAppMessageViewEventHandlerFactory,
) {

    fun process(view: InAppMessageView, event: InAppMessageViewEvent, types: List<InAppMessageViewEventHandleType>) {
        for (handleType in types) {
            try {
                val handler = handlerFactory.get(handleType)
                handler.handle(view, event)
            } catch (e: Throwable) {
                log.error { "Failed to handle InAppMessage view event [$handleType]: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<InAppMessageViewEventHandleProcessor>()
    }
}
