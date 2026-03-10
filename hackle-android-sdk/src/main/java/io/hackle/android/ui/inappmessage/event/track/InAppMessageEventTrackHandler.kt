package io.hackle.android.ui.inappmessage.event.track

import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageEventTrackHandler(
    private val tracker: InAppMessageEventTracker,
) : InAppMessageEventHandler {
    override fun supports(handleType: InAppMessageEventHandleType): Boolean {
        return handleType == InAppMessageEventHandleType.TRACK
    }

    override fun handle(view: InAppMessageView, event: InAppMessageEvent) {
        tracker.track(view.presentationContext, event, event.timestamp)
    }
}
