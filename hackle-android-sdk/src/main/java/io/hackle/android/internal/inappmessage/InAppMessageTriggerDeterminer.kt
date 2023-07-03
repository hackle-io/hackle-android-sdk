package io.hackle.android.internal.inappmessage

import io.hackle.android.internal.utils.anyOrEmpty
import io.hackle.sdk.core.evaluation.evaluator.Evaluator
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageTriggerDeterminer(
    private val targetMatcher: TargetMatcher
) {


    fun determine(
        inAppMessages: List<InAppMessage>,
        track: UserEvent.Track,
        workspace: Workspace
    ): List<InAppMessage> {
        return inAppMessages
            .filter { isTriggered(it, track, workspace) }
    }

    private fun isTriggered(
        inAppMessage: InAppMessage,
        track: UserEvent.Track,
        workspace: Workspace
    ): Boolean {
        return inAppMessage.eventTriggerRules
            .filter { it.eventKey == track.event.key }
            .any { rule ->
                rule.targets.anyOrEmpty {
                    targetMatcher.matches(
                        InAppMessageRequest.of(
                            inAppMessage.key,
                            track,
                            workspace
                        ),
                        Evaluators.context(),
                        it
                    )
                }
            }
    }

    class InAppMessageRequest(
        override val event: UserEvent.Track,
        override val key: Evaluator.Key,
        override val user: HackleUser,
        override val workspace: Workspace
    ) : Evaluator.EventRequest {

        companion object {
            fun of(
                inAppMessageKey: Long,
                track: UserEvent.Track,
                workspace: Workspace
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
}