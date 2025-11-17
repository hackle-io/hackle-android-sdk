package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout

/**
 * Controller interface for handling [InAppMessageLayout]
 */
internal interface InAppMessageController {

    /**
     * The [InAppMessageLayout] instance managed by this controller.
     */
    val layout: InAppMessageLayout

    /**
     * The context in which [InAppMessageLayout] is presented.
     */
    val context: InAppMessagePresentationContext

    /**
     * Used to present, interact and manage [InAppMessageLayout].
     */
    val ui: InAppMessageUi

    /**
     * Opens the [InAppMessageLayout] on the [activity].
     */
    fun open(activity: Activity)

    /**
     * Closes the [InAppMessageLayout].
     * @param whenActivityDestroy Close the [InAppMessageLayout] when the activity is destroyed. Default is `false`.
     */
    fun close(whenActivityDestroy: Boolean = false)
}

internal fun InAppMessageController.handle(event: InAppMessageEvent) {
    ui.eventHandler.handle(layout, event)
}
