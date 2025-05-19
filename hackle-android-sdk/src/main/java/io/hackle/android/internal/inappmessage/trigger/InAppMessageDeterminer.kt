package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.trigger.InAppMessageDeterminer.Companion.log
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageDeterminer(
    private val workspaceFetcher: WorkspaceFetcher,
    private val eventMatcher: InAppMessageEventMatcher,
    private val core: HackleCore,
) {

    fun determineOrNull(event: UserEvent): InAppMessagePresentationContext? {
        val workspace = workspaceFetcher.fetch() ?: return null
        return workspace.inAppMessages.asSequence()
            .filter { eventMatcher.matches(workspace, it, event) }
            .mapNotNull { context(it, event) }
            .firstOrNull()
    }

    private fun context(
        inAppMessage: InAppMessage,
        event: UserEvent
    ): InAppMessagePresentationContext? {
        val decision = core.tryInAppMessage(inAppMessage.key, event.user)
        val properties = PropertiesBuilder()
            .add(decision.properties)
            .add("decision_reason", decision.reason.name)
            .add("trigger_event_insert_id", event.insertId)
            .build()

        log.debug { "InAppMessage [${inAppMessage.key}]: ${decision.reason}" }
        return InAppMessagePresentationContext(
            inAppMessage = decision.inAppMessage ?: return null,
            message = decision.message ?: return null,
            user = event.user,
            properties = properties,
            decisionReason = decision.reason
        )
    }

    companion object {
        val log = Logger<InAppMessageDeterminer>()
    }
}

internal fun HackleCore.tryInAppMessage(
    inAppMessageKey: Long,
    user: HackleUser,
): InAppMessageDecision {
    val sample = Timer.start()
    return try {
        inAppMessage(inAppMessageKey, user)
    } catch (t: Throwable) {
        log.error { "Unexpected error while deciding in app message [$inAppMessageKey]: $t" }
        InAppMessageDecision.of(DecisionReason.EXCEPTION)
    }.also {
        DecisionMetrics.inAppMessage(sample, inAppMessageKey, it)
    }
}
