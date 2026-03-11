package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.listener

internal interface InAppMessageViewEventActor<EVENT : InAppMessageViewEvent> {
    fun supports(event: InAppMessageViewEvent): Boolean
    fun action(view: InAppMessageView, event: EVENT)
}

internal class InAppMessageEventActorFactory(private val actors: List<InAppMessageViewEventActor<out InAppMessageViewEvent>>) {
    fun get(event: InAppMessageViewEvent): InAppMessageViewEventActor<InAppMessageViewEvent>? {
        @Suppress("UNCHECKED_CAST")
        return actors.find { it.supports(event) } as? InAppMessageViewEventActor<InAppMessageViewEvent>
    }
}

internal class InAppMessageImpressionViewEventActor : InAppMessageViewEventActor<InAppMessageViewEvent.Impression> {
    override fun supports(event: InAppMessageViewEvent): Boolean {
        return event is InAppMessageViewEvent.Impression
    }

    override fun action(view: InAppMessageView, event: InAppMessageViewEvent.Impression) {
        // Do nothing.
    }
}

internal class InAppMessageActionViewEventActor(
    private val actionHandlerFactory: InAppMessageActionHandlerFactory,
) : InAppMessageViewEventActor<InAppMessageViewEvent.Action> {
    override fun supports(event: InAppMessageViewEvent): Boolean {
        return event is InAppMessageViewEvent.Action
    }

    override fun action(view: InAppMessageView, event: InAppMessageViewEvent.Action) {
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

internal class InAppMessageCloseViewEventActor : InAppMessageViewEventActor<InAppMessageViewEvent.Close> {
    override fun supports(event: InAppMessageViewEvent): Boolean {
        return event is InAppMessageViewEvent.Close
    }

    override fun action(view: InAppMessageView, event: InAppMessageViewEvent.Close) {
        // Do nothing. This method is called after the view is closed.
    }
}
