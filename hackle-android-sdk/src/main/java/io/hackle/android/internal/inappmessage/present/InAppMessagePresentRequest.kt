package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessagePresentRequest(
    val dispatchId: String,
    val workspace: Workspace,
    val inAppMessage: InAppMessage,
    val user: HackleUser,
    val requestedAt: Long,
    val evaluation: InAppMessageEvaluation,
    val properties: Map<String, Any>,
) {
    override fun toString(): String {
        return "InAppMessagePresentRequest(dispatchId=$dispatchId, inAppMessage=$inAppMessage, user=${user.identifiers}, requestedAt=$requestedAt, evaluation=$evaluation, properties=$properties)"
    }

    companion object {
        fun of(
            request: InAppMessageDeliverRequest,
            workspace: Workspace,
            inAppMessage: InAppMessage,
            user: HackleUser,
            evaluation: InAppMessageEvaluation,
        ): InAppMessagePresentRequest {
            return InAppMessagePresentRequest(
                dispatchId = request.dispatchId,
                workspace = workspace,
                inAppMessage = inAppMessage,
                user = user,
                requestedAt = request.requestedAt,
                evaluation = evaluation,
                properties = request.properties
            )
        }
    }
}
