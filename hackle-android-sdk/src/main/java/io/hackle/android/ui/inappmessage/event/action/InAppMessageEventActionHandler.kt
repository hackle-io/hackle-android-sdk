package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageEventActionHandler(
    private val actorFactory: InAppMessageEventActorFactory,
) : InAppMessageEventHandler {

    override fun supports(handleType: InAppMessageEventHandleType): Boolean {
        return handleType == InAppMessageEventHandleType.ACTION
    }

    override fun handle(view: InAppMessageView, event: InAppMessageEvent) {
        val actor = actorFactory.get(event) ?: return
        actor.action(view, event)
    }
}
