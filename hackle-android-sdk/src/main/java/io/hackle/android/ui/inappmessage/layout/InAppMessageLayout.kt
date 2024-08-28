package io.hackle.android.ui.inappmessage.layout

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.android.ui.inappmessage.listener
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.common.HackleInAppMessageView
import io.hackle.sdk.core.model.InAppMessage

/**
 * Base layout interface for [InAppMessage].
 */
internal interface InAppMessageLayout : HackleInAppMessageView {

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

    /**
     * Closes the [InAppMessageLayout]
     */
    override fun close() {
        controller.close()
    }
}

internal val InAppMessageLayout.listener: HackleInAppMessageListener get() = controller.listener

internal fun InAppMessageLayout.handle(event: InAppMessageEvent) {
    controller.handle(event)
}
