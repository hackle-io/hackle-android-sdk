package io.hackle.android.internal.inappmessage

import io.hackle.android.internal.event.UserEventListener
import io.hackle.android.internal.inappmessage.reset.InAppMessageResetProcessor
import io.hackle.android.internal.inappmessage.trigger.InAppMessageTriggerProcessor
import io.hackle.android.internal.user.UserListener
import io.hackle.sdk.common.User
import io.hackle.sdk.core.event.UserEvent

internal class InAppMessageManager(
    private val triggerProcessor: InAppMessageTriggerProcessor,
    private val resetProcessor: InAppMessageResetProcessor,
) : UserEventListener, UserListener {

    override fun onEvent(event: UserEvent) {
        triggerProcessor.process(event)
    }

    override fun onUserUpdated(oldUser: User, newUser: User, timestamp: Long) {
        resetProcessor.process(oldUser, newUser)
    }
}
