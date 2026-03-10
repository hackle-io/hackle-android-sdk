package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageEventHandleProcessor(
    private val handlerFactory: InAppMessageEventHandlerFactory,
) {

    fun process(view: InAppMessageView, event: InAppMessageEvent, types: List<InAppMessageEventHandleType>) {
        for (handleType in types) {
            val handler = handlerFactory.get(handleType)
            handler.handle(view, event)
        }
    }
}
