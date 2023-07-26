package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.HackleActivityManager
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresenter
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewFactory
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicBoolean


/**
 * This class is used to display in-app messages, handle events, and manage [InAppMessageView].
 * Only one in-app message is displayed at a time.
 *
 * Note that this class is managed as single instance.
 * @see InAppMessageUi.instance
 */
internal class InAppMessageUi(
    private val hackleActivityManager: HackleActivityManager,
    private val messageViewFactory: InAppMessageViewFactory,
    val eventHandler: InAppMessageEventHandler,
) : InAppMessagePresenter {

    var currentMessageView: InAppMessageView? = null
        private set

    private val opening = AtomicBoolean(false)

    override fun present(context: InAppMessagePresentationContext) {
        runOnUiThread {
            if (!opening.compareAndSet(false, true)) return@runOnUiThread
            try {
                presentNow(context)
            } finally {
                opening.set(false)
            }
        }
    }

    private fun presentNow(context: InAppMessagePresentationContext) {
        val activity = hackleActivityManager.currentActivity ?: return
        if (currentMessageView != null) return
        if (!isSupportedOrientation(activity, context)) return

        var messageView: InAppMessageView? = null
        try {
            messageView = messageViewFactory.create(context, this)
            this.currentMessageView = messageView
            messageView.open(activity)
        } catch (e: Throwable) {
            log.error { "Failed to present InAppMessage: $e" }
            messageView?.close()
        }
    }

    private fun isSupportedOrientation(activity: Activity, context: InAppMessagePresentationContext): Boolean {
        val orientation = activity.orientation ?: return false
        return context.inAppMessage.supports(orientation)
    }

    fun closeCurrent() {
        currentMessageView = null
    }

    companion object {

        private val log = Logger<InAppMessageUi>()
        private var INSTANCE: InAppMessageUi? = null

        fun create(
            hackleActivityManager: HackleActivityManager,
            messageViewFactory: InAppMessageViewFactory,
            eventHandler: InAppMessageEventHandler,
        ): InAppMessageUi {
            return INSTANCE
                ?: InAppMessageUi(hackleActivityManager, messageViewFactory, eventHandler)
                    .also { INSTANCE = it }
        }

        val instance: InAppMessageUi get() = requireNotNull(INSTANCE) { "InAppMessageUi not initialized" }
    }
}
