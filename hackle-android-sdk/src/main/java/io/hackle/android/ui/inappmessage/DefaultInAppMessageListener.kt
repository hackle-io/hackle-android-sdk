package io.hackle.android.ui.inappmessage

import io.hackle.sdk.common.HackleInAppMessage
import io.hackle.sdk.common.HackleInAppMessageAction
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.common.HackleInAppMessageView

internal object DefaultInAppMessageListener : HackleInAppMessageListener {
    override fun beforeInAppMessageOpen(inAppMessage: HackleInAppMessage) {}
    override fun afterInAppMessageOpen(inAppMessage: HackleInAppMessage) {}
    override fun beforeInAppMessageClose(inAppMessage: HackleInAppMessage) {}
    override fun afterInAppMessageClose(inAppMessage: HackleInAppMessage) {}
    override fun onInAppMessageClick(
        inAppMessage: HackleInAppMessage,
        view: HackleInAppMessageView,
        action: HackleInAppMessageAction
    ): Boolean = false
}
