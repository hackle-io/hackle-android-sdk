package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle.DESTROYED
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycleListener
import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.core.ImageLoader
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleProcessor
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewProvider
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


/**
 * This class is used to display in-app messages, handle events, and manage [InAppMessageView].
 * Only one in-app message is displayed at a time.
 *
 * Note that this class is managed as single instance.
 * @see InAppMessageUi.instance
 */
internal class InAppMessageUi(
    private val activityProvider: ActivityProvider,
    private val messageControllerFactory: InAppMessageControllerFactory,
    private val defaultListener: HackleInAppMessageListener,
    val clock: Clock,
    val scheduler: Scheduler,
    val eventHandleProcessor: InAppMessageViewEventHandleProcessor,
    val imageLoader: ImageLoader,
) : InAppMessagePresenter, InAppMessageViewProvider, ActivityLifecycleListener {

    private val _currentMessageController = AtomicReference<InAppMessageController>()
    private val currentMessageController: InAppMessageController? get() = _currentMessageController.get()

    private var customListener: HackleInAppMessageListener? = null
    val listener get() = customListener ?: defaultListener

    private var _isBackButtonDismisses: Boolean = true
    val isBackButtonDismisses get() = _isBackButtonDismisses

    private val opening = AtomicBoolean(false)

    override val currentView: InAppMessageView? get() = currentMessageController?.view

    override fun getView(id: String): InAppMessageView? {
        val view = currentView ?: return null
        return if (view.id == id) view else null
    }

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

    fun setListener(listener: HackleInAppMessageListener?) {
        customListener = listener
    }

    fun setBackButtonDismisses(backButtonDismissesInAppMessage: Boolean) {
        _isBackButtonDismisses = backButtonDismissesInAppMessage
    }

    fun closeCurrent() {
        _currentMessageController.set(null)
    }

    override fun onLifecycle(
        activityLifecycle: ActivityLifecycle,
        activity: Activity,
        timestamp: Long,
    ) {
        if (activityLifecycle != DESTROYED) {
            return
        }
        val controller = currentMessageController ?: return
        val controllerActivity = controller.view.activity
        if (controllerActivity != null && controllerActivity != activity) {
            return
        }
        try {
            controller.close(true)
        } catch (e: Exception) {
            log.debug { "Failed to close message on destroyed activity: $e" }
        }
    }

    companion object {

        private val log = Logger<InAppMessageUi>()
        private var INSTANCE: InAppMessageUi? = null

        fun create(
            activityProvider: ActivityProvider,
            messageControllerFactory: InAppMessageControllerFactory,
            clock: Clock,
            scheduler: Scheduler,
            eventProcessor: InAppMessageViewEventHandleProcessor,
            imageLoader: ImageLoader,
        ): InAppMessageUi {
            return INSTANCE
                ?: InAppMessageUi(
                    activityProvider,
                    messageControllerFactory,
                    DefaultInAppMessageListener,
                    clock,
                    scheduler,
                    eventProcessor,
                    imageLoader
                ).also { INSTANCE = it }
        }

        val instance: InAppMessageUi get() = requireNotNull(INSTANCE) { "InAppMessageUi not initialized" }
    }
}
