package io.hackle.android.inappmessage

import android.content.Intent
import io.hackle.android.inappmessage.activity.InAppMessageActivity
import io.hackle.android.internal.HackleActivityManager
import io.hackle.android.internal.inappmessage.InAppMessageRenderSource
import io.hackle.android.internal.utils.toJson


internal class InAppMessageRenderer(
    private val hackleActivityManager: HackleActivityManager
) {

    fun render(
        inAppMessageRenderSource: InAppMessageRenderSource
    ) {
        val activity = hackleActivityManager.currentActivity ?: return

        val intent = Intent(activity, InAppMessageActivity::class.java)
        val inAppMessage = inAppMessageRenderSource.inAppMessage.toJson()
        val message = inAppMessageRenderSource.message.toJson()

        intent.putExtra("message", message)
        intent.putExtra("inAppMessage", inAppMessage)

        activity.startActivity(intent)
    }

}