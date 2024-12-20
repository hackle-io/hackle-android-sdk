package io.hackle.android.ui.inappmessage

import io.hackle.sdk.common.HackleInAppMessage
import io.hackle.sdk.common.HackleInAppMessageAction
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.common.HackleInAppMessageView

internal enum class InAppMessageLifecycle {
    BEFORE_OPEN,
    AFTER_OPEN,
    BEFORE_CLOSE,
    AFTER_CLOSE
}

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

internal fun HackleInAppMessageListener.onLifecycle(
    lifecycle: InAppMessageLifecycle,
    inAppMessage: HackleInAppMessage
) {
    return when (lifecycle) {
        InAppMessageLifecycle.BEFORE_OPEN -> beforeInAppMessageOpen(inAppMessage)
        InAppMessageLifecycle.AFTER_OPEN -> afterInAppMessageOpen(inAppMessage)
        InAppMessageLifecycle.BEFORE_CLOSE -> beforeInAppMessageClose(inAppMessage)
        InAppMessageLifecycle.AFTER_CLOSE -> afterInAppMessageClose(inAppMessage)
    }
}
