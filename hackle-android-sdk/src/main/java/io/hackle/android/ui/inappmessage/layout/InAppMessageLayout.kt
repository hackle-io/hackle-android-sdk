package io.hackle.android.ui.inappmessage.layout

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.sdk.core.model.InAppMessage


/**
 * Base layout interface for [InAppMessage].
 */
internal interface InAppMessageLayout {

    /**
     * The controller that manages the lifecycle and interactions of this [InAppMessageLayout].
     */
    val controller: InAppMessageController

    /**
     * The context in which this [InAppMessageLayout] is presented.
     */
    val context: InAppMessagePresentationContext

    /**
     * The [Activity] where [InAppMessageLayout] is presented.
     */
    val activity: Activity?
}

internal fun InAppMessageLayout.close() {
    controller.close()
}

internal fun InAppMessageLayout.handle(event: InAppMessageEvent) {
    controller.handle(event)
}
