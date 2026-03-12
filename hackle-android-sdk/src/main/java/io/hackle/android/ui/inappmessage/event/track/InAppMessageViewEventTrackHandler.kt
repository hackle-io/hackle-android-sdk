package io.hackle.android.ui.inappmessage.event.track

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView

internal class InAppMessageViewEventTrackHandler(
    private val tracker: InAppMessageEventTracker,
) : InAppMessageViewEventHandler {
    override fun supports(handleType: InAppMessageViewEventHandleType): Boolean {
        return handleType == InAppMessageViewEventHandleType.TRACK
    }

    override fun handle(view: InAppMessageView, event: InAppMessageViewEvent) {
        tracker.track(view.presentationContext, event)
    }
}
