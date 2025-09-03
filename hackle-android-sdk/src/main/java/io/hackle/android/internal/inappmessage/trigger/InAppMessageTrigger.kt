package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage

internal data class InAppMessageTrigger(
    val inAppMessage: InAppMessage,
    val reason: DecisionReason,
    val event: UserEvent.Track,
) {
    override fun toString(): String {
        return "InAppMessageTrigger(inAppMessage=$inAppMessage, reason=$reason, insertId=${event.insertId}, timestamp=${event.timestamp}, user=${event.user.identifiers}, event=${event.event})"
    }
}
