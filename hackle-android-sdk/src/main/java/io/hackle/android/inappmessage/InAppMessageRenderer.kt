package io.hackle.android.inappmessage

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import io.hackle.android.inappmessage.activity.InAppMessageActivity
import io.hackle.android.inappmessage.base.InAppMessageTrack
import io.hackle.android.internal.HackleActivityManager
import io.hackle.android.internal.inappmessage.InAppMessageRenderSource
import io.hackle.android.internal.utils.toJson
import io.hackle.sdk.core.model.InAppMessage
import java.util.concurrent.atomic.AtomicBoolean


internal class InAppMessageRenderer(
    private val hackleActivityManager: HackleActivityManager,
) {
    private val displayingInAppMessage = AtomicBoolean(false)

    fun render(source: InAppMessageRenderSource) {
        renderNow(source)
    }

    private fun renderNow(source: InAppMessageRenderSource) {

        val activity = hackleActivityManager.currentActivity ?: return

        if (!isSupportedOrientation(activity, source)) {
            return
        }

        if (!displayingInAppMessage.compareAndSet(false, true)) {
            return
        }

        val intent = Intent(activity, InAppMessageActivity::class.java)
        val inAppMessage = source.inAppMessage
        val message = source.message.toJson()
        val messageContext = inAppMessage.messageContext.toJson()

        intent.putExtra("messageContext", messageContext)
        intent.putExtra("message", message)
        intent.putExtra("inAppMessageId", inAppMessage.id)
        intent.putExtra("inAppMessageKey", inAppMessage.key)

        activity.startActivity(intent)
        InAppMessageTrack.impressionTrack(source)
    }


    private fun isSupportedOrientation(
        activity: Activity,
        source: InAppMessageRenderSource,
    ): Boolean {
        val orientation = activity.orientation ?: return false
        return orientation in source.inAppMessage.messageContext.orientations
    }

    private val Activity.orientation: InAppMessage.MessageContext.Orientation?
        get() {
            return when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> InAppMessage.MessageContext.Orientation.VERTICAL
                Configuration.ORIENTATION_LANDSCAPE -> InAppMessage.MessageContext.Orientation.HORIZONTAL
                else -> null
            }
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