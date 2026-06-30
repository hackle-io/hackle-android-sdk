package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.evaluation.eligibility
import io.hackle.android.internal.workspace.config.WorkspaceConfigManager
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.TRIGGER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.InAppMessageConfig

internal class LocalInAppMessageTriggerDeterminer(
    override val eventMatcher: InAppMessageEventMatcher,
    private val workspaceManager: WorkspaceConfigManager,
    private val evaluateProcessor: EvaluateProcessor,
) : AbstractInAppMessageTriggerDeterminer<WorkspaceConfig, InAppMessageConfig>() {

    override fun workspace(user: HackleUser): WorkspaceConfig? {
        return workspaceManager.workspace(user)
    }

    override fun evaluate(
        workspace: WorkspaceConfig,
        message: InAppMessageConfig,
        event: UserEvent,
    ): InAppMessageEligibilityEvaluation {
        val response = evaluateProcessor.eligibility(workspace, message, event.user, TRIGGER, event.timestamp)
        return response.evaluation
    }
}
