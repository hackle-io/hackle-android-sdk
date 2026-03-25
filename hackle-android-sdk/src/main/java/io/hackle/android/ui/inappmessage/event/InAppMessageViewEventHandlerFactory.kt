package io.hackle.android.ui.inappmessage.event

internal class InAppMessageViewEventHandlerFactory(
    private val handlers: List<InAppMessageViewEventHandler>,
) {

    fun get(handleType: InAppMessageViewEventHandleType): InAppMessageViewEventHandler {
        val handler = handlers.find { it.supports(handleType) }
        return requireNotNull(handler) { "Not found InAppMessageEventHandler [$handleType]" }
    }
}
