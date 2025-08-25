package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluator
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageTriggerDeterminer(
    private val workspaceFetcher: WorkspaceFetcher,
    private val eventMatcher: InAppMessageEventMatcher,
    private val evaluator: InAppMessageEvaluator,
) {
    fun determine(event: UserEvent): InAppMessageTrigger? {
        val trackEvent = event as? UserEvent.Track ?: return null
        val workspace = workspaceFetcher.fetch() ?: return null

        for (inAppMessage in workspace.inAppMessages) {
            val matches = eventMatcher.matches(workspace, inAppMessage, trackEvent)
            if (!matches) {
                continue
            }

            val evaluation = evaluator.evaluate(workspace, inAppMessage, event.user, event.timestamp)
            if (evaluation.isEligible) {
                return InAppMessageTrigger(inAppMessage, evaluation, trackEvent)
            }
        }

        return null
    }
}
