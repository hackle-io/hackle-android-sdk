package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageViewEventHandleProcessor(
    private val handlerFactory: InAppMessageViewEventHandlerFactory,
) {

    fun process(view: InAppMessageView, event: InAppMessageViewEvent, types: List<InAppMessageViewEventHandleType>) {
        for (handleType in types) {
            // 한 핸들러(트래킹/액션/호스트 콜백)의 예외가 다른 핸들러와 UI 클릭 콜백으로 전파되지 않도록 격리한다.
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
