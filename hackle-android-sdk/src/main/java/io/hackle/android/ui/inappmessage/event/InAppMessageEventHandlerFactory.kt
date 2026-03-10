package io.hackle.android.ui.inappmessage.event

internal class InAppMessageEventHandlerFactory(
    private val handlers: List<InAppMessageEventHandler>,
) {

    fun get(handleType: InAppMessageEventHandleType): InAppMessageEventHandler {
        val handler = handlers.find { it.supports(handleType) }
        return requireNotNull(handler) { "Not found InAppMessageEventHandler [$handleType]" }
    }
}
