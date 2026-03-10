package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.listener

internal interface InAppMessageEventActor<EVENT : InAppMessageEvent> {
    fun supports(event: InAppMessageEvent): Boolean
    fun action(view: InAppMessageView, event: EVENT)
}

internal class InAppMessageEventActorFactory(private val actors: List<InAppMessageEventActor<out InAppMessageEvent>>) {
    fun get(event: InAppMessageEvent): InAppMessageEventActor<InAppMessageEvent>? {
        @Suppress("UNCHECKED_CAST")
        return actors.find { it.supports(event) } as? InAppMessageEventActor<InAppMessageEvent>
    }
}

internal class InAppMessageImpressionEventActor : InAppMessageEventActor<InAppMessageEvent.Impression> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Impression
    }

    override fun action(view: InAppMessageView, event: InAppMessageEvent.Impression) {
        // Do nothing.
    }
}

internal class InAppMessageActionEventActor(
    private val actionHandlerFactory: InAppMessageActionHandlerFactory,
) : InAppMessageEventActor<InAppMessageEvent.Action> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Action
    }

    override fun action(view: InAppMessageView, event: InAppMessageEvent.Action) {
        val isProcessed = view.listener.onInAppMessageClick(view.inAppMessage, view, event.action)
        if (isProcessed) {
            return
        }

        if (view.state != InAppMessageView.State.OPENED) {
            return
        }

        val handler = actionHandlerFactory.get(event.action) ?: return
        handler.handle(view, event.action)
    }
}

internal class InAppMessageCloseEventActor : InAppMessageEventActor<InAppMessageEvent.Close> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Close
    }

    override fun action(view: InAppMessageView, event: InAppMessageEvent.Close) {
        // Do nothing. This method is called after the view is closed.
    }
}
