package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresenter
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.core.ImageLoader
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


/**
 * This class is used to display in-app messages, handle events, and manage [InAppMessageLayout].
 * Only one in-app message is displayed at a time.
 *
 * Note that this class is managed as single instance.
 * @see InAppMessageUi.instance
 */
internal class InAppMessageUi(
    private val activityProvider: ActivityProvider,
    private val messageControllerFactory: InAppMessageControllerFactory,
    val eventHandler: InAppMessageEventHandler,
    val imageLoader: ImageLoader
) : InAppMessagePresenter {

    private val _currentMessageController = AtomicReference<InAppMessageController>()
    val currentMessageController: InAppMessageController? get() = _currentMessageController.get()

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
        val activity = activityProvider.currentActivity ?: return
        if (currentMessageController != null) return
        if (!isSupportedOrientation(activity, context)) return

        var messageController: InAppMessageController? = null
        try {
            messageController = messageControllerFactory.create(context, this, activity) ?: return
            _currentMessageController.set(messageController)
            messageController.open(activity)
        } catch (e: Throwable) {
            log.error { "Failed to present InAppMessage: $e" }
            messageController?.close()
        }
    }

    private fun isSupportedOrientation(activity: Activity, context: InAppMessagePresentationContext): Boolean {
        val orientation = activity.orientation ?: return false
        return context.inAppMessage.supports(orientation)
    }

    fun closeCurrent() {
        _currentMessageController.set(null)
    }

    companion object {

        private val log = Logger<InAppMessageUi>()
        private var INSTANCE: InAppMessageUi? = null

        fun create(
            activityProvider: ActivityProvider,
            messageControllerFactory: InAppMessageControllerFactory,
            eventHandler: InAppMessageEventHandler,
            imageLoader: ImageLoader,
        ): InAppMessageUi {
            return INSTANCE
                ?: InAppMessageUi(activityProvider, messageControllerFactory, eventHandler, imageLoader)
                    .also { INSTANCE = it }
        }

        val instance: InAppMessageUi get() = requireNotNull(INSTANCE) { "InAppMessageUi not initialized" }
    }
}
