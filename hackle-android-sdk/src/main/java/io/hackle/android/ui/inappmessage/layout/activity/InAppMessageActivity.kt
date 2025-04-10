package io.hackle.android.ui.inappmessage.layout.activity

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.ui.HackleActivity
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivityController.Companion.IN_APP_MESSAGE_ID
import io.hackle.sdk.core.model.InAppMessage

internal abstract class InAppMessageActivity : FragmentActivity(), HackleActivity, InAppMessageLayout {

    private lateinit var _controller: InAppMessageActivityController

    override val activity: Activity get() = this
    override val state: InAppMessageLayout.State get() = controller.state
    override val controller: InAppMessageActivityController get() = _controller
    override val context: InAppMessagePresentationContext get() = controller.context
    val inAppMessage: InAppMessage get() = context.inAppMessage
    val message: InAppMessage.Message get() = context.message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!initialize()) {
            finish()
            return
        }
        setFinishOnTouchOutside(true)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        layout()
    }

    override fun publish(lifecycle: InAppMessageLifecycle) {}

    private fun initialize(): Boolean {
        val messageController = InAppMessageUi.instance.currentMessageController ?: return false
        if (messageController !is InAppMessageActivityController) {
            messageController.close()
            return false
        }

        val messageId = intent.getLongExtra(IN_APP_MESSAGE_ID, -1)
        if (messageController.context.inAppMessage.id != messageId) {
            messageController.close()
            return false
        }

        this._controller = messageController
        messageController.onOpen(this)
        return true
    }

    protected abstract fun layout()
}
