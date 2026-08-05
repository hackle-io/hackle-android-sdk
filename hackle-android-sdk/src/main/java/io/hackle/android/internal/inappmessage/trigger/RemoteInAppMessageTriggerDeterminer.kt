package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.evaluation.eligibility
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.TRIGGER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult

internal class RemoteInAppMessageTriggerDeterminer(
    override val eventMatcher: InAppMessageEventMatcher,
    private val workspaceManager: WorkspaceEvaluationManager,
    private val evaluateProcessor: EvaluateProcessor,
) : AbstractInAppMessageTriggerDeterminer<WorkspaceEvaluation, InAppMessageEligibilityRemoteEvaluateResult>() {

    override fun workspace(user: HackleUser): WorkspaceEvaluation? {
        return workspaceManager.workspace(user)
    }

    override fun evaluate(
        workspace: WorkspaceEvaluation,
        message: InAppMessageEligibilityRemoteEvaluateResult,
        event: UserEvent,
    ): InAppMessageEligibilityEvaluation {
        val response = evaluateProcessor.eligibility(workspace, message, event.user, TRIGGER, event.timestamp)
        return response.evaluation
    }
}
