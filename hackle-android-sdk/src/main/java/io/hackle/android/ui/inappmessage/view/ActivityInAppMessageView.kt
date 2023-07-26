package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.content.Intent
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.activity.InAppMessageActivity
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent

internal class ActivityInAppMessageView(
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi
) : InAppMessageView {

    override val activity: Activity? get() = messageActivity
    private var messageActivity: InAppMessageActivity? = null

    override fun open(activity: Activity) {
        val intent = Intent(activity, InAppMessageActivity::class.java)
        intent.putExtra(MESSAGE_ID, context.inAppMessage.id)
        activity.startActivity(intent)
    }

    fun onOpen(activity: InAppMessageActivity) {
        handle(InAppMessageEvent.Impression)
        messageActivity = activity
    }

    override fun close() {
        handle(InAppMessageEvent.Close)
        messageActivity?.finish()
        messageActivity = null
        ui.closeCurrent()
    }

    companion object {
        const val MESSAGE_ID = "inAppMessageId"
    }
}
