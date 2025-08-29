package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluation
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
) {
    override fun toString(): String {
        return "InAppMessagePresentRequest(dispatchId=$dispatchId, inAppMessage=$inAppMessage, user=${user.identifiers}, requestedAt=$requestedAt, reason=$reason, properties=$properties)"
    }

    companion object {
        fun of(
            request: InAppMessageDeliverRequest,
            inAppMessage: InAppMessage,
            user: HackleUser,
            eligibilityEvaluation: InAppMessageEligibilityEvaluation,
            layoutEvaluation: InAppMessageLayoutEvaluation,
        ): InAppMessagePresentRequest {
            return InAppMessagePresentRequest(
                dispatchId = request.dispatchId,
                inAppMessage = inAppMessage,
                message = layoutEvaluation.message,
                user = user,
                requestedAt = request.requestedAt,
                reason = eligibilityEvaluation.reason,
                properties = request.properties + layoutEvaluation.properties
            )
        }
    }
}
