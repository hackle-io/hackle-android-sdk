package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.eligibility
import io.hackle.android.internal.inappmessage.evaluation.layout
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.workspace.config.WorkspaceConfigManager
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.DELIVER
import io.hackle.sdk.core.user.HackleUser
import java.util.concurrent.CompletableFuture

internal class InAppMessageDeliverLocalEvaluator(
    private val workspaceManager: WorkspaceConfigManager,
    private val evaluateProcessor: EvaluateProcessor,
) : InAppMessageDeliverEvaluator {
    override fun evaluate(
        request: InAppMessageDeliverRequest,
        user: HackleUser,
    ): CompletableFuture<InAppMessageDeliverEvaluateResponse> {
        return Futures.sync { doEvaluate(request, user) }
    }

    private fun doEvaluate(
        request: InAppMessageDeliverRequest,
        user: HackleUser,
    ): InAppMessageDeliverEvaluateResponse {
        val workspace = workspaceManager.workspace(user)
            ?: return InAppMessageDeliverEvaluateResponse.ineligible(Code.WORKSPACE_NOT_FOUND)

        val inAppMessage = workspace.getInAppMessageOrNull(request.inAppMessageKey)
            ?: return InAppMessageDeliverEvaluateResponse.ineligible(Code.IN_APP_MESSAGE_NOT_FOUND)

        val layout = evaluateProcessor.layout(workspace, inAppMessage, user, DELIVER)
        val eligibility = evaluateProcessor.eligibility(workspace, inAppMessage, user, DELIVER, request.requestedAt)
        val evaluation = InAppMessageDeliverEvaluation(eligibility, layout)
        return InAppMessageDeliverEvaluateResponse.of(evaluation)
    }
}
