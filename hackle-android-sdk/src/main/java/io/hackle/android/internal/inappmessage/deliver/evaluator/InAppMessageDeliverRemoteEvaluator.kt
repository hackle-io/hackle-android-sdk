package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.eligibility
import io.hackle.android.internal.inappmessage.evaluation.layout
import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.task.map
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.DELIVER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateResponse
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult
import java.util.concurrent.CompletableFuture


internal class InAppMessageDeliverRemoteEvaluator(
    private val workspaceManager: WorkspaceEvaluationManager,
    private val evaluateProcessor: EvaluateProcessor,
) : InAppMessageDeliverEvaluator {
    override fun evaluate(
        request: InAppMessageDeliverRequest,
        user: HackleUser,
    ): CompletableFuture<InAppMessageDeliverEvaluateResponse> {
        val workspace = workspaceManager.workspace(user)
            ?: return InAppMessageDeliverEvaluateResponse.ineligible(Code.WORKSPACE_NOT_FOUND).asFuture()

        val inAppMessage = workspace.getInAppMessageOrNull(request.inAppMessageKey)
            ?: return InAppMessageDeliverEvaluateResponse.ineligible(Code.IN_APP_MESSAGE_NOT_FOUND).asFuture()

        return resolveWorkspaceEvaluation(request, workspace, inAppMessage, user)
            .map { evaluate(request, it, user) }
    }

    private fun resolveWorkspaceEvaluation(
        request: InAppMessageDeliverRequest,
        workspace: WorkspaceEvaluation,
        inAppMessage: InAppMessageEligibilityRemoteEvaluateResult,
        user: HackleUser,
    ): CompletableFuture<WorkspaceEvaluation> {
        if (inAppMessage.evaluateContext.atDeliverTime) {
            // time + dedup 만 먼저 evaluate 해서 API 호출 최적화
            val response = evaluateProcessor.eligibility(request, workspace, inAppMessage, user, record = false)
            if (!response.evaluation.result.isEligible) {
                return workspace.asFuture()
            }
            return workspaceManager.evaluate(RemoteEvaluateContext.of(user), listOf(inAppMessage))
        } else {
            return workspace.asFuture()
        }
    }

    private fun evaluate(
        request: InAppMessageDeliverRequest,
        workspace: WorkspaceEvaluation,
        user: HackleUser,
    ): InAppMessageDeliverEvaluateResponse {
        val inAppMessage = workspace.getInAppMessageOrNull(request.inAppMessageKey)
            ?: return InAppMessageDeliverEvaluateResponse.ineligible(Code.IN_APP_MESSAGE_NOT_FOUND)
        val layout = evaluateProcessor.layout(workspace, inAppMessage, user)
        val eligibility = evaluateProcessor.eligibility(request, workspace, inAppMessage, user, record = true)
        val evaluation = InAppMessageDeliverEvaluation(eligibility, layout)
        return InAppMessageDeliverEvaluateResponse.of(evaluation)
    }

    private fun EvaluateProcessor.eligibility(
        request: InAppMessageDeliverRequest,
        workspace: WorkspaceEvaluation,
        inAppMessage: InAppMessageEligibilityRemoteEvaluateResult,
        user: HackleUser,
        record: Boolean,
    ): InAppMessageEligibilityEvaluateResponse {
        return eligibility(workspace, inAppMessage, user, DELIVER, request.requestedAt, record)
    }

    private fun EvaluateProcessor.layout(
        workspace: WorkspaceEvaluation,
        inAppMessage: InAppMessageEligibilityRemoteEvaluateResult,
        user: HackleUser,
    ): InAppMessageLayoutEvaluateResponse {
        return layout(workspace, inAppMessage, user, DELIVER)
    }
}
