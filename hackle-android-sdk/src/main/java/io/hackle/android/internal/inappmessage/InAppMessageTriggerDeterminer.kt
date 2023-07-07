package io.hackle.android.internal.inappmessage

import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.evaluation.evaluator.Evaluator
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageTriggerDeterminer(
    private val core: HackleCore,
    private val targetMatcher: TargetMatcher,
) {

    fun determine(
        inAppMessages: List<InAppMessage>,
        userEvent: UserEvent,
        workspace: Workspace,
    ): InAppMessageRenderSource? {

        if (userEvent !is UserEvent.Track) {
            return null
        }

        for (inAppMessage in inAppMessages) {
            if (isTriggeredEvent(inAppMessage, userEvent, workspace)) {
                return source(inAppMessage, userEvent) ?: continue
            }
        }

        return null
    }

    private fun isTriggeredEvent(
        inAppMessage: InAppMessage,
        track: UserEvent.Track,
        workspace: Workspace,
    ): Boolean {
        return inAppMessage.eventTriggerRules
            .any { matches(InAppMessageRequest.of(inAppMessage.key, track, workspace), it, track) }
    }

    private fun matches(
        request: InAppMessageRequest,
        rule: InAppMessage.EventTriggerRule,
        event: UserEvent.Track,
    ): Boolean {
        if (rule.eventKey != event.event.key) {
            return false
        }

        if (rule.targets.isEmpty()) {
            return true
        }

        return rule.targets.any {
            targetMatcher.matches(request, Evaluators.context(), it)
        }
    }

    private fun source(
        inAppMessage: InAppMessage,
        userEvent: UserEvent,
    ): InAppMessageRenderSource? {
        val decision = core.tryInAppMessage(inAppMessage.key, userEvent.user)
        if (!decision.isShow) {
            return null
        }
        return InAppMessageRenderSource(
            inAppMessage = decision.inAppMessage ?: return null,
            message = decision.message ?: return null,
            properties = PropertiesBuilder().add("decision_reason", decision.reason.name).build()
        )
    }

    private fun HackleCore.tryInAppMessage(
        inAppMessageKey: Long,
        user: HackleUser,
    ): InAppMessageDecision {
        val sample = Timer.start()

        val decision = try {
            inAppMessage(inAppMessageKey, user)
        } catch (e: Exception) {
            log.error { "Unexpected error while deciding in app message $e" }
            InAppMessageDecision(reason = DecisionReason.EXCEPTION)
        }

        DecisionMetrics.inAppMessage(sample, inAppMessageKey, decision)

        return decision
    }


    class InAppMessageRequest(
        override val event: UserEvent.Track,
        override val key: Evaluator.Key,
        override val user: HackleUser,
        override val workspace: Workspace,
    ) : Evaluator.EventRequest {

        companion object {
            fun of(
                inAppMessageKey: Long,
                track: UserEvent.Track,
                workspace: Workspace,
            ): InAppMessageRequest {
                return InAppMessageRequest(
                    event = track,
                    key = Evaluator.Key(Evaluator.Type.IN_APP_MESSAGE, inAppMessageKey),
                    user = track.user,
                    workspace = workspace
                )
            }
        }
    }

    companion object {
        private val log = Logger<InAppMessageTriggerDeterminer>()
    }
}