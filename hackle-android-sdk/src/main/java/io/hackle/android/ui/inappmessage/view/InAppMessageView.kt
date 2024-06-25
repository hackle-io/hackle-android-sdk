package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.sdk.core.model.InAppMessage


/**
 * Base view interface for [InAppMessage].
 */
internal interface InAppMessageView {

    /**
     * The controller that manages the lifecycle and interactions of this [InAppMessageView].
     */
    val controller: InAppMessageController

    /**
     * The context in which this [InAppMessageView] is presented.
     */
    val context: InAppMessagePresentationContext

    /**
     * The [Activity] where [InAppMessageView] is presented.
     */
    val activity: Activity?
}

internal fun InAppMessageView.close() {
    controller.close()
}

internal fun InAppMessageView.handle(event: InAppMessageEvent) {
    controller.handle(event)
}
