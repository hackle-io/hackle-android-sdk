package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageViewEventActionHandler(
    private val actorFactory: InAppMessageEventActorFactory,
) : InAppMessageViewEventHandler {

    override fun supports(handleType: InAppMessageViewEventHandleType): Boolean {
        return handleType == InAppMessageViewEventHandleType.ACTION
    }

    override fun handle(view: InAppMessageView, event: InAppMessageViewEvent) {
        val actor = actorFactory.get(event) ?: return
        actor.action(view, event)
    }
}
