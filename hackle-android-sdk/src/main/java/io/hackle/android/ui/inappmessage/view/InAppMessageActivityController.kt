package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.content.Intent
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle

internal class InAppMessageActivityController private constructor(
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi,
    private val viewType: Class<out InAppMessageActivityView>
) : InAppMessageController {

    private var activityView: InAppMessageActivityView? = null
    override val view: InAppMessageView get() = checkNotNull(activityView) { "InAppMessageActivityView not opened [${viewType.simpleName}]" }

    override fun open(activity: Activity) {
        val intent = Intent(activity, viewType)
        intent.putExtra(IN_APP_MESSAGE_ID, context.inAppMessage.id)
        activity.startActivity(intent)
    }

    fun onOpen(activityView: InAppMessageActivityView) {
        this.activityView = activityView
        handle(InAppMessageEvent.Impression)
    }

    override fun close() {
        handle(InAppMessageEvent.Close)
        activityView?.activity?.finish()
        activityView = null
        ui.closeCurrent()
    }

    companion object {
        const val IN_APP_MESSAGE_ID = "hackleInAppMessageId"
        inline fun <reified VIEW : InAppMessageActivityView> create(
            context: InAppMessagePresentationContext,
            ui: InAppMessageUi
        ): InAppMessageActivityController {
            return InAppMessageActivityController(context, ui, VIEW::class.java)
        }
    }
}
