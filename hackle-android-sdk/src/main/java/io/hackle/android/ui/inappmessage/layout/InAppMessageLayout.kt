package io.hackle.android.ui.inappmessage.layout

import android.app.Activity
import android.view.View
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
 * Base layout interface for [InAppMessage].
 */
internal interface InAppMessageLayout : HackleInAppMessageView {

    /**
     * The current state of the [InAppMessageLayout]
     */
    val state: State

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
     * Publishes the lifecycle event to child views.
     */
    fun publish(lifecycle: InAppMessageLifecycle)

    /**
     * Closes the [InAppMessageLayout]
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
    if (this is InAppMessageLayout.LifecycleListener) {
        onLifeCycle(lifecycle)
    }

    if (this is ViewGroup) {
        for (view in children) {
            view.publishInAppMessageLifecycle(lifecycle)
        }
    }
}

internal fun InAppMessageLayout.LifecycleListener.onLifeCycle(lifecycle: InAppMessageLifecycle) {
    return when (lifecycle) {
        InAppMessageLifecycle.BEFORE_OPEN -> beforeInAppMessageOpen()
        InAppMessageLifecycle.AFTER_OPEN -> afterInAppMessageOpen()
        InAppMessageLifecycle.BEFORE_CLOSE -> beforeInAppMessageClose()
        InAppMessageLifecycle.AFTER_CLOSE -> afterInAppMessageClose()
    }
}

internal fun InAppMessageLayout.handle(event: InAppMessageEvent) {
    controller.handle(event)
}

internal val InAppMessageLayout.listener get() = controller.ui.listener
