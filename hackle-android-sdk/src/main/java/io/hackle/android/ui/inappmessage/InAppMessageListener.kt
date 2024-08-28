package io.hackle.android.ui.inappmessage

import io.hackle.sdk.common.HackleInAppMessage
import io.hackle.sdk.common.HackleInAppMessageAction
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.common.HackleInAppMessageView

internal object InAppMessageListener : HackleInAppMessageListener {
    override fun onWillOpen(inAppMessage: HackleInAppMessage) {}
    override fun onDidOpen(inAppMessage: HackleInAppMessage) {}
    override fun onWillClose(inAppMessage: HackleInAppMessage) {}
    override fun onDidClose(inAppMessage: HackleInAppMessage) {}
    override fun onClick(
        view: HackleInAppMessageView,
        inAppMessage: HackleInAppMessage,
        action: HackleInAppMessageAction
    ): Boolean = false
}
