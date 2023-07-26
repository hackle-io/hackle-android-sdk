package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageEventMatcher(
    ruleDeterminer: InAppMessageEventTriggerDeterminer,
    frequencyCapDeterminer: InAppMessageEventTriggerDeterminer
) {

    private val determiners = listOf(ruleDeterminer, frequencyCapDeterminer)

    fun matches(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent): Boolean {
        val trackEvent = event as? UserEvent.Track ?: return false
        return determiners.all {
            it.isTriggerTarget(workspace, inAppMessage, trackEvent)
        }
    }
}
