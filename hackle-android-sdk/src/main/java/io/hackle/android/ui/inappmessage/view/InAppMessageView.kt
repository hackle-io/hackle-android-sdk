package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.ACTION
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.TRACK
import io.hackle.android.ui.inappmessage.handle
import io.hackle.sdk.common.HackleInAppMessageView
import io.hackle.sdk.core.model.InAppMessage

/**
 * Base view interface for [InAppMessage].
 */
internal interface InAppMessageView : HackleInAppMessageView {

    /**
     * The unique identifier of this view.
     */
    val id: String

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
     * The [InAppMessage] displayed by this view.
     */
    override val inAppMessage: InAppMessage get() = presentationContext.inAppMessage

    /**
     * Sets up the view with message content and calls [ReadyListener.onReady] when ready to show.
     */
    fun configure(listener: ReadyListener)

    /**
     * Publishes the lifecycle event and propagates it to child views that implement [LifecycleListener].
     *
     * @see publishInAppMessageLifecycle
     */
    fun publish(lifecycle: InAppMessageLifecycle)

    /**
     * Closes the [InAppMessageView]
     */
    override fun close() {
        controller.close()
    }

    /**
     * Lifecycle state of the [InAppMessageView].
     *
     * State moves in one direction only and cannot go back.
     *
     * ```
     * ┌─────────┐ open()  ┌─────────┐            ┌────────┐ close()   ┌────────┐
     * │ CREATED │ ------> │ OPENING │ ---------> │ OPENED │ --------> │ CLOSED │
     * └─────────┘         └─────────┘ onReady()  └────────┘           └────────┘
     *                         |           ^
     *                         v           |
     *                    configure() -----┘
     * ```
     */
    enum class State {
        CREATED,
        OPENING,
        OPENED,
        CLOSED
    }

    /**
     * Listener that is called when the view is ready to show.
     */
    fun interface ReadyListener {
        fun onReady()
    }

    /**
     * Handles lifecycle events propagated via [publish].
     */
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

internal val InAppMessageView.message: InAppMessage.Message get() = presentationContext.message
internal val InAppMessageView.clock get() = controller.ui.clock
internal val InAppMessageView.listener get() = controller.ui.listener

internal fun InAppMessageView.handle(
    event: InAppMessageViewEvent,
    types: List<InAppMessageViewEventHandleType> = listOf(TRACK, ACTION)
) {
    controller.handle(event, types)
}

internal fun InAppMessageView.handle(event: InAppMessageViewEvent, type: InAppMessageViewEventHandleType) {
    handle(event, listOf(type))
}
