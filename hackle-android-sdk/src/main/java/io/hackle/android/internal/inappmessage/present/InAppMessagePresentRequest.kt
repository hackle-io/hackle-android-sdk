package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluation
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser

internal class InAppMessagePresentRequest(
    val dispatchId: String,
    val inAppMessage: InAppMessage,
    val message: InAppMessage.Message,
    val user: HackleUser,
    val requestedAt: Long,
    val reason: DecisionReason,
    val properties: Map<String, Any>,
    val triggerEvent: Event,
) {
    override fun toString(): String {
        return "InAppMessagePresentRequest(dispatchId=$dispatchId, inAppMessage=$inAppMessage, message=${message.layout.displayType}, user=${user.identifiers}, requestedAt=$requestedAt, reason=$reason, triggerEventKey=${triggerEvent.key}, properties=$properties)"
    }

    companion object {
        fun of(
            request: InAppMessageDeliverRequest,
            user: HackleUser,
            evaluation: InAppMessageDeliverEvaluation,
        ): InAppMessagePresentRequest {
            return InAppMessagePresentRequest(
                dispatchId = request.dispatchId,
                inAppMessage = evaluation.eligibility.evaluation.entity,
                message = evaluation.layout.evaluation.result.message,
                user = user,
                requestedAt = request.requestedAt,
                reason = evaluation.eligibility.evaluation.result.reason,
                properties = request.properties + evaluation.toProperties(),
                triggerEvent = request.triggerEvent
            )
        }
    }
}
