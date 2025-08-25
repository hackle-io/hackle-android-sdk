package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluation
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser

internal data class InAppMessagePresentationContext(
    val dispatchId: String,
    val inAppMessage: InAppMessage,
    val message: InAppMessage.Message,
    val user: HackleUser,
    val decisionReason: DecisionReason,
    val properties: Map<String, Any>,
) {

    override fun toString(): String {
        return "InAppMessagePresentationContext(dispatchId=$dispatchId, inAppMessage=$inAppMessage, message=$message)"
    }

    companion object {
        fun of(
            request: InAppMessagePresentRequest,
            evaluation: InAppMessageLayoutEvaluation,
        ): InAppMessagePresentationContext {
            return InAppMessagePresentationContext(
                dispatchId = request.dispatchId,
                inAppMessage = evaluation.inAppMessage,
                message = evaluation.message,
                user = request.user,
                decisionReason = request.evaluation.reason,
                properties = request.properties + evaluation.properties
            )
        }
    }
}
