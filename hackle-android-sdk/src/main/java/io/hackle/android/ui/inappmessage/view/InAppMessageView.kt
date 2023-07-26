package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.sdk.core.model.InAppMessage


/**
 * Base view interface for [InAppMessage].
 */
internal interface InAppMessageView {

    /**
     * The [Activity] where [InAppMessageView] is presented.
     *
     * Can be different from the activity parameters passed on [open].
     */
    val activity: Activity?

    /**
     * [InAppMessage] context to present.
     */
    val context: InAppMessagePresentationContext

    /**
     * Used to present, interact and manage [InAppMessageView].
     */
    val ui: InAppMessageUi

    /**
     * Opens an [InAppMessage] on the [activity].
     */
    fun open(activity: Activity)

    /**
     * Closes an [InAppMessage].
     */
    fun close()
}

internal fun InAppMessageView.handle(event: InAppMessageEvent) {
    ui.eventHandler.handle(this, event)
}
