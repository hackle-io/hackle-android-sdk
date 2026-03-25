package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageViewEventHandleProcessor(
    private val handlerFactory: InAppMessageViewEventHandlerFactory,
) {

    fun process(view: InAppMessageView, event: InAppMessageViewEvent, types: List<InAppMessageViewEventHandleType>) {
        for (handleType in types) {
            val handler = handlerFactory.get(handleType)
            handler.handle(view, event)
        }
    }
}
