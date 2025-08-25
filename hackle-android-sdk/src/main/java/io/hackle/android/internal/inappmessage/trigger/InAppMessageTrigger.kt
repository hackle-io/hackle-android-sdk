package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage

internal data class InAppMessageTrigger(
    val inAppMessage: InAppMessage,
    val evaluation: InAppMessageEvaluation,
    val event: UserEvent.Track,
) {
    override fun toString(): String {
        return "InAppMessageTrigger(inAppMessage=$inAppMessage, evaluation=$evaluation, insertId=${event.insertId}, timestamp=${event.timestamp}, user=${event.user.identifiers}, event=${event.event})"
    }
}
