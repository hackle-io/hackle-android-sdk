package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal interface InAppMessageTriggerDeterminer {
    fun determine(event: UserEvent): InAppMessageTrigger?
}

internal abstract class AbstractInAppMessageTriggerDeterminer<WORKSPACE : Workspace, MESSAGE : InAppMessage> :
    InAppMessageTriggerDeterminer {
    protected abstract val eventMatcher: InAppMessageEventMatcher

    protected abstract fun workspace(user: HackleUser): WORKSPACE?
    protected abstract fun evaluate(
        workspace: WORKSPACE,
        message: MESSAGE,
        event: UserEvent,
    ): InAppMessageEligibilityEvaluation

    final override fun determine(event: UserEvent): InAppMessageTrigger? {
        val trackEvent = event as? UserEvent.Track ?: return null
        val workspace = workspace(event.user) ?: return null
        for (message in workspace.inAppMessages) {
            val matches = eventMatcher.matches(workspace, message, trackEvent)
            if (!matches) {
                continue
            }
            @Suppress("UNCHECKED_CAST")
            val evaluation = evaluate(workspace, message as MESSAGE, event)
            if (evaluation.result.isEligible) {
                return InAppMessageTrigger(message, evaluation.result.reason, trackEvent)
            }
        }
        return null
    }
}
