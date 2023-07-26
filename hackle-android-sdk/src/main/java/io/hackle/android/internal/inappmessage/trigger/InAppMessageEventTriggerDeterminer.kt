package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.storage.InAppMessageImpression
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpressionStorage
import io.hackle.sdk.core.evaluation.evaluator.Evaluator
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal interface InAppMessageEventTriggerDeterminer {
    fun isTriggerTarget(
        workspace: Workspace,
        inAppMessage: InAppMessage,
        event: UserEvent.Track
    ): Boolean
}

internal class InAppMessageEventTriggerRuleDeterminer(
    private val targetMatcher: TargetMatcher
) : InAppMessageEventTriggerDeterminer {

    override fun isTriggerTarget(
        workspace: Workspace,
        inAppMessage: InAppMessage,
        event: UserEvent.Track
    ): Boolean {
        return inAppMessage.eventTrigger.rules.any {
            ruleMatches(workspace, event, inAppMessage, it)
        }
    }

    private fun ruleMatches(
        workspace: Workspace,
        event: UserEvent.Track,
        inAppMessage: InAppMessage,
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
                    key = Evaluator.Key(Evaluator.Type.IN_APP_MESSAGE, inAppMessage.key),
                    workspace = workspace,
                    user = event.user,
                    event = event
                )
            }
        }
    }
}

internal class InAppMessageEventTriggerFrequencyCapDeterminer(
    private val storage: InAppMessageImpressionStorage
) : InAppMessageEventTriggerDeterminer {
    override fun isTriggerTarget(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent.Track): Boolean {

        val frequencyCap = inAppMessage.eventTrigger.frequencyCap ?: return true

        val contexts = createMatchContexts(frequencyCap)
        if (contexts.isEmpty()) {
            return true
        }

        val impressions = storage.get(inAppMessage)
        for (impression in impressions) {
            for (context in contexts) {
                if (context.matches(event, impression)) {
                    return false
                }
            }
        }
        return true
    }

    private fun createMatchContexts(frequencyCap: InAppMessage.EventTrigger.FrequencyCap): List<MatchContext> {
        val contexts = mutableListOf<MatchContext>()
        for (identifierCap in frequencyCap.identifierCaps) {
            val predicate = IdentifierCapPredicate(identifierCap)
            contexts.add(MatchContext(predicate))
        }
        val durationCap = frequencyCap.durationCap
        if (durationCap != null) {
            contexts.add(MatchContext(DurationCapPredicate(durationCap)))
        }

        return contexts
    }

    private class MatchContext(private val predicate: FrequencyCapPredicate) {
        private var matchCount = 0

        fun matches(event: UserEvent, impression: InAppMessageImpression): Boolean {
            if (predicate.matches(event, impression)) {
                matchCount++
            }
            return matchCount >= predicate.thresholdCount
        }
    }

    interface FrequencyCapPredicate {
        val thresholdCount: Int
        fun matches(event: UserEvent, impression: InAppMessageImpression): Boolean
    }

    class IdentifierCapPredicate(
        private val identifierCap: InAppMessage.EventTrigger.IdentifierCap
    ) : FrequencyCapPredicate {
        override val thresholdCount: Int get() = identifierCap.count

        override fun matches(event: UserEvent, impression: InAppMessageImpression): Boolean {
            val userIdentifier = event.user.identifiers[identifierCap.identifierType] ?: return false
            val impressionIdentifier = impression.identifiers[identifierCap.identifierType] ?: return false
            return userIdentifier == impressionIdentifier
        }
    }

    class DurationCapPredicate(
        private val durationCap: InAppMessage.EventTrigger.DurationCap
    ) : FrequencyCapPredicate {
        override val thresholdCount: Int get() = durationCap.count

        override fun matches(event: UserEvent, impression: InAppMessageImpression): Boolean {
            return (event.timestamp - impression.timestamp) <= durationCap.durationMillis
        }
    }
}
