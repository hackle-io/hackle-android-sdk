package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView

/**
 * Handles [InAppMessageViewEvent] occurred in [InAppMessageView].
 */
internal interface InAppMessageViewEventHandler {
    fun supports(handleType: InAppMessageViewEventHandleType): Boolean
    fun handle(view: InAppMessageView, event: InAppMessageViewEvent)
}
