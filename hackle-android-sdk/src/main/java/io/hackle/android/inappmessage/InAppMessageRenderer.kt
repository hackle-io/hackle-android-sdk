package io.hackle.android.inappmessage

import android.content.Intent
import io.hackle.android.inappmessage.activity.InAppMessageActivity
import io.hackle.android.internal.HackleActivityManager
import io.hackle.android.internal.inappmessage.InAppMessageRenderSource
import io.hackle.android.internal.utils.toJson
import java.util.concurrent.atomic.AtomicBoolean


internal class InAppMessageRenderer (
    private val hackleActivityManager: HackleActivityManager
) {
    private val displayingInAppMessage = AtomicBoolean(false)
    fun render(inAppMessageRenderSource: InAppMessageRenderSource) {

        if (!displayingInAppMessage.compareAndSet(false, true)) {
            return
        }

        val activity = hackleActivityManager.currentActivity ?: return

        val intent = Intent(activity, InAppMessageActivity::class.java)
        val inAppMessage = inAppMessageRenderSource.inAppMessage
        val message = inAppMessageRenderSource.message.toJson()
        val messageContext = inAppMessage.messageContext.toJson()

        intent.putExtra("messageContext", messageContext)
        intent.putExtra("message", message)
        intent.putExtra("inAppMessageId", inAppMessage.id)
        intent.putExtra("inAppMessageKey", inAppMessage.key)
        activity.startActivity(intent)
    }

    fun closeCurrent() {
        displayingInAppMessage.set(false)
    }

    companion object {

        private var INSTANCE: InAppMessageRenderer? = null
        fun create(hackleActivityManager: HackleActivityManager): InAppMessageRenderer {
            return INSTANCE ?: InAppMessageRenderer(hackleActivityManager).also {
                INSTANCE = it
            }
        }

        fun getInstance(): InAppMessageRenderer {
            return requireNotNull(INSTANCE)
        }
    }
}