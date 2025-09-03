package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateProcessor
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateType
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityRequest
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageTriggerDeterminer(
    private val workspaceFetcher: WorkspaceFetcher,
    private val eventMatcher: InAppMessageEventMatcher,
    private val evaluateProcessor: InAppMessageEvaluateProcessor,
) {
    fun determine(event: UserEvent): InAppMessageTrigger? {
        val trackEvent = event as? UserEvent.Track ?: return null
        val workspace = workspaceFetcher.fetch() ?: return null

        for (inAppMessage in workspace.inAppMessages) {
            val matches = eventMatcher.matches(workspace, inAppMessage, trackEvent)
            if (!matches) {
                continue
            }

            val evaluation = evaluate(workspace, inAppMessage, event)
            if (evaluation.isEligible) {
                return InAppMessageTrigger(inAppMessage, evaluation.reason, trackEvent)
            }
        }

        return null
    }

    private fun evaluate(
        workspace: Workspace,
        inAppMessage: InAppMessage,
        event: UserEvent.Track,
    ): InAppMessageEligibilityEvaluation {
        val request = InAppMessageEligibilityRequest(workspace, event.user, inAppMessage, event.timestamp)
        return evaluateProcessor.process(InAppMessageEvaluateType.TRIGGER, request)
    }
}
