package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageEventMatcher(
    private val ruleMatcher: InAppMessageEventTriggerRuleMatcher,
) {
    fun matches(workspace: Workspace, inAppMessage: InAppMessage, event: UserEvent.Track): Boolean {
        return ruleMatcher.matches(workspace, inAppMessage, event)
    }
}
