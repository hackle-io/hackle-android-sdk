package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView

/**
 * Handles [InAppMessageEvent] occurred in [InAppMessageView].
 */
internal interface InAppMessageEventHandler {
    fun supports(handleType: InAppMessageEventHandleType): Boolean
    fun handle(view: InAppMessageView, event: InAppMessageEvent)
}
