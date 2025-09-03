package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.listener

internal interface InAppMessageEventProcessor<EVENT : InAppMessageEvent> {
    fun supports(event: InAppMessageEvent): Boolean
    fun process(layout: InAppMessageLayout, event: EVENT, timestamp: Long)
}

internal class InAppMessageEventProcessorFactory(private val processors: List<InAppMessageEventProcessor<out InAppMessageEvent>>) {
    fun get(event: InAppMessageEvent): InAppMessageEventProcessor<InAppMessageEvent>? {
        @Suppress("UNCHECKED_CAST")
        return processors.find { it.supports(event) } as? InAppMessageEventProcessor<InAppMessageEvent>
    }
}

internal class InAppMessageImpressionEventProcessor : InAppMessageEventProcessor<InAppMessageEvent.Impression> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Impression
    }

    override fun process(
        layout: InAppMessageLayout,
        event: InAppMessageEvent.Impression,
        timestamp: Long,
    ) {
        // Do nothing.
    }
}

internal class InAppMessageActionEventProcessor(
    private val actionHandlerFactory: InAppMessageActionHandlerFactory,
) : InAppMessageEventProcessor<InAppMessageEvent.Action> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Action
    }

    override fun process(
        layout: InAppMessageLayout,
        event: InAppMessageEvent.Action,
        timestamp: Long,
    ) {
        val isProcessed =
            layout.listener.onInAppMessageClick(layout.context.inAppMessage, layout, event.action)
        if (isProcessed) {
            return
        }

        if (layout.state == InAppMessageLayout.State.CLOSED) {
            return
        }

        val handler = actionHandlerFactory.get(event.action) ?: return
        handler.handle(layout, event.action)
    }
}

internal class InAppMessageCloseEventProcessor :
    InAppMessageEventProcessor<InAppMessageEvent.Close> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Close
    }

    override fun process(
        layout: InAppMessageLayout,
        event: InAppMessageEvent.Close,
        timestamp: Long,
    ) {
        // Do nothing. This method is called after the layout is closed.
    }
}
