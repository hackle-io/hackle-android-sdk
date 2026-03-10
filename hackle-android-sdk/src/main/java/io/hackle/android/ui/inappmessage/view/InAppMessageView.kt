package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.view.children
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.sdk.common.HackleInAppMessageView
import io.hackle.sdk.core.model.InAppMessage

/**
 * Base view interface for [InAppMessage].
 */
internal interface InAppMessageView : HackleInAppMessageView {

    /**
     * The current state of the [InAppMessageView]
     */
    val state: State

    /**
     * The controller that manages the lifecycle and interactions of this [InAppMessageView].
     */
    val controller: InAppMessageController

    /**
     * The context in which this [InAppMessageView] is presented.
     */
    val presentationContext: InAppMessagePresentationContext

    /**
     * The [Activity] where [InAppMessageView] is presented.
     */
    val activity: Activity?

    /**
     * Publishes the lifecycle event to child views.
     */
    fun publish(lifecycle: InAppMessageLifecycle)

    /**
     * Closes the [InAppMessageView]
     */
    override fun close() {
        controller.close()
    }

    enum class State {
        OPENED, CLOSED
    }

    interface LifecycleListener {
        fun beforeInAppMessageOpen() {}
        fun afterInAppMessageOpen() {}
        fun beforeInAppMessageClose() {}
        fun afterInAppMessageClose() {}
    }
}

internal fun View.publishInAppMessageLifecycle(lifecycle: InAppMessageLifecycle) {
    if (this is InAppMessageView.LifecycleListener) {
        onLifeCycle(lifecycle)
    }

    if (this is ViewGroup) {
        for (view in children) {
            view.publishInAppMessageLifecycle(lifecycle)
        }
    }
}

internal fun InAppMessageView.LifecycleListener.onLifeCycle(lifecycle: InAppMessageLifecycle) {
    return when (lifecycle) {
        InAppMessageLifecycle.BEFORE_OPEN -> beforeInAppMessageOpen()
        InAppMessageLifecycle.AFTER_OPEN -> afterInAppMessageOpen()
        InAppMessageLifecycle.BEFORE_CLOSE -> beforeInAppMessageClose()
        InAppMessageLifecycle.AFTER_CLOSE -> afterInAppMessageClose()
    }
}

internal val InAppMessageView.inAppMessage: InAppMessage get() = presentationContext.inAppMessage
internal val InAppMessageView.message: InAppMessage.Message get() = presentationContext.message

internal fun InAppMessageView.handle(event: InAppMessageEvent) {
    controller.handle(event)
}

internal val InAppMessageView.listener get() = controller.ui.listener

internal fun InAppMessageView.createCloseListener(): OnClickListener {
    return OnClickListener { close() }
}

internal fun InAppMessageView.createMessageClickListener(): OnClickListener {
    return OnClickListener {
        val action = message.action ?: return@OnClickListener
        handle(InAppMessageEvent.messageAction(action))
    }
}

internal fun InAppMessageView.createImageClickListener(
    image: InAppMessage.Message.Image,
    order: Int?,
): OnClickListener {
    return OnClickListener {
        val action = image.action ?: return@OnClickListener
        handle(InAppMessageEvent.imageAction(action, image, order))
    }
}
