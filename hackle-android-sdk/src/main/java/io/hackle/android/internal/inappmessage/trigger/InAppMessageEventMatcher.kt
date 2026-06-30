package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.match.EventEvaluateRequest
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.Entity
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageEventMatcher(
    private val targetMatcher: TargetMatcher,
) {
    fun matches(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent.Track): Boolean {
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
        val request = InAppMessageEventEvaluateRequest.of(workspace, inAppMessage, event)
        return targetMatcher.anyMatches(request, Evaluators.context(), rule.targets)
    }
}

internal class InAppMessageEventEvaluateRequest(
    override val user: HackleUser,
    override val workspace: Workspace,
    override val entity: Entity,
    override val event: UserEvent,
) : EventEvaluateRequest {
    override val record: Boolean get() = false

    companion object {
        fun of(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent): InAppMessageEventEvaluateRequest {
            return InAppMessageEventEvaluateRequest(
                workspace = workspace,
                entity = inAppMessage,
                user = event.user,
                event = event
            )
        }
    }
}
