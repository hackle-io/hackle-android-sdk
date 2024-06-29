package io.hackle.android.ui.inappmessage.layout.activity

import android.app.Activity
import android.content.Intent
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.inappmessage.InAppMessageController
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.handle
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout

internal class InAppMessageActivityController private constructor(
    override val context: InAppMessagePresentationContext,
    override val ui: InAppMessageUi,
    private val activityClass: Class<out InAppMessageActivity>
) : InAppMessageController {

    private var messageActivity: InAppMessageActivity? = null
    override val layout: InAppMessageLayout get() = checkNotNull(messageActivity) { "InAppMessageActivity not opened [${activityClass.simpleName}]" }

    override fun open(activity: Activity) {
        val intent = Intent(activity, activityClass)
        intent.putExtra(IN_APP_MESSAGE_ID, context.inAppMessage.id)
        activity.startActivity(intent)
    }

    fun onOpen(messageActivity: InAppMessageActivity) {
        this.messageActivity = messageActivity
        handle(InAppMessageEvent.Impression)
    }

    override fun close() {
        handle(InAppMessageEvent.Close)
        messageActivity?.activity?.finish()
        messageActivity = null
        ui.closeCurrent()
    }

    companion object {
        const val IN_APP_MESSAGE_ID = "hackleInAppMessageId"
        inline fun <reified ACTIVITY : InAppMessageActivity> create(
            context: InAppMessagePresentationContext,
            ui: InAppMessageUi
        ): InAppMessageActivityController {
            return InAppMessageActivityController(context, ui, ACTIVITY::class.java)
        }
    }
}
