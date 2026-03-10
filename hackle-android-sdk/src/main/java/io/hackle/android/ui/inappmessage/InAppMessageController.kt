package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandleType.ACTION
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandleType.TRACK
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

/**
 * Handles the [event] with the given [types].
 */
internal fun InAppMessageController.handle(
    event: InAppMessageEvent,
    types: List<InAppMessageEventHandleType> = listOf(TRACK, ACTION)
) {
    ui.eventHandleProcessor.process(view, event, types)
}
