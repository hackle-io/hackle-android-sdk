package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.view.InAppMessageView

/**
 * Controller interface for handling [InAppMessageView]
 */
internal interface InAppMessageController {

    /**
     * The [InAppMessageView] instance managed by this controller.
     */
    val view: InAppMessageView

    /**
     * Used to present, interact and manage [InAppMessageView].
     */
    val ui: InAppMessageUi

    /**
     * Opens the [InAppMessageView] on the [activity].
     */
    fun open(activity: Activity)

    /**
     * Closes the [InAppMessageView].
     * @param whenActivityDestroy Close the [InAppMessageView] when the activity is destroyed. Default is `false`.
     */
    fun close(whenActivityDestroy: Boolean = false)
}

internal fun InAppMessageController.handle(event: InAppMessageEvent) {
    ui.eventHandler.handle(view, event)
}
