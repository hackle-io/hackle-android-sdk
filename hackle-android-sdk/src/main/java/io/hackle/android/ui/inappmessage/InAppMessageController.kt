package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.model.InAppMessage

/**
 * Controller interface for handling [InAppMessageView]
 */
internal interface InAppMessageController {

    /**
     * The [InAppMessageView] instance managed by this controller.
     */
    val view: InAppMessageView

    /**
     * The context in which [InAppMessageView] is presented.
     */
    val context: InAppMessagePresentationContext

    /**
     * Used to present, interact and manage [InAppMessageView].
     */
    val ui: InAppMessageUi

    /**
     * Opens the [InAppMessage] on the [activity].
     */
    fun open(activity: Activity)

    /**
     * Closes the [InAppMessage].
     */
    fun close()
}

internal fun InAppMessageController.handle(event: InAppMessageEvent) {
    ui.eventHandler.handle(view, event)
}
