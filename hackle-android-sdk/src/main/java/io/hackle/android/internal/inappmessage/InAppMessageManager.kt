package io.hackle.android.internal.inappmessage

import io.hackle.android.inappmessage.InAppMessageRenderer
import io.hackle.android.inappmessage.base.InAppMessageTrack.impressionTrack
import io.hackle.android.internal.event.EventListener
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.monitoring.metric.DecisionMetrics
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Timer
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageManager(
    private val workspaceFetcher: WorkspaceFetcher,
    private val core: HackleCore,
    private val appStateManager: AppStateManager,
    private val inAppMessageRenderer: InAppMessageRenderer,
    private val inAppMessageTriggerDeterminer: InAppMessageTriggerDeterminer
) : EventListener {

    private val currentState: AppState
        get() = appStateManager.currentState

    override fun onEventPublish(userEvent: UserEvent) {
        val workspace = workspaceFetcher.fetch() ?: let {
            log.warn { "SDK not ready." }
            return
        }

        if (userEvent !is UserEvent.Track) {
            return
        }

        val triggeredInAppMessages =
            inAppMessageTriggerDeterminer.determine(workspace.inAppMessages, userEvent, workspace)

        val inAppMessageRenderSource = triggeredInAppMessages
            .map { core.tryInAppMessage(it.key, userEvent.user) }
            .mapNotNull { it.toInAppMessageRenderSource() }
            .firstOrNull() ?: return

        if (currentState == AppState.FOREGROUND) {
            inAppMessageRenderer.render(
                inAppMessageRenderSource
            ).also {
                impressionTrack(
                    inAppMessageRenderSource.inAppMessage
                )
            }
        }
    }

    private fun InAppMessageDecision.toInAppMessageRenderSource(): InAppMessageRenderSource? {
        if (inAppMessage != null && message != null) {
            return InAppMessageRenderSource(inAppMessage!!, message!!)
        }
        return null
    }

    private fun HackleCore.tryInAppMessage(
        inAppMessageKey: Long,
        user: HackleUser
    ): InAppMessageDecision {
        val sample = Timer.start()

        val decision = try {
            inAppMessage(inAppMessageKey, user)
        } catch (e: Exception) {
            log.error { "Unexpected error while deciding in app message $e" }
            InAppMessageDecision(reason = DecisionReason.EXCEPTION, false)
        }

        DecisionMetrics.inAppMessage(sample, inAppMessageKey, decision)

        return decision
    }


    companion object {
        private val log = Logger<InAppMessageManager>()
    }
}