package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.evaluation.evaluator.Evaluator
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal interface InAppMessageEventTriggerMatcher {
    fun matches(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent.Track): Boolean
}

internal class InAppMessageEventTriggerRuleMatcher(
    private val targetMatcher: TargetMatcher,
) : InAppMessageEventTriggerMatcher {
    override fun matches(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent.Track): Boolean {
        return inAppMessage.eventTrigger.rules.any {
            matches(workspace, inAppMessage, event, it)
        }
    }

    private fun matches(
        workspace: Workspace,
        inAppMessage: InAppMessage,
        event: UserEvent.Track,
        rule: InAppMessage.EventTrigger.Rule,
    ): Boolean {
        if (event.event.key != rule.eventKey) {
            return false
        }
        val request = EvaluatorRequest.of(workspace, event, inAppMessage)
        return targetMatcher.anyMatches(request, Evaluators.context(), rule.targets)
    }

    private class EvaluatorRequest private constructor(
        override val key: Evaluator.Key,
        override val workspace: Workspace,
        override val user: HackleUser,
        override val event: UserEvent,
    ) : Evaluator.EventRequest {

        companion object {
            fun of(
                workspace: Workspace,
                event: UserEvent,
                inAppMessage: InAppMessage,
            ): EvaluatorRequest {
                return EvaluatorRequest(
                    key = Evaluator.Key(Evaluator.Type.IN_APP_MESSAGE, inAppMessage.id),
                    workspace = workspace,
                    user = event.user,
                    event = event
                )
            }
        }
    }
}