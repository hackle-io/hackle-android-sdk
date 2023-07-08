package io.hackle.android.internal.inappmessage

import io.hackle.android.internal.event.EventListener
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.ui.inappmessage.InAppMessageRenderer
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageManager(
    private val workspaceFetcher: WorkspaceFetcher,
    private val appStateManager: AppStateManager,
    private val inAppMessageRenderer: InAppMessageRenderer,
    private val inAppMessageTriggerDeterminer: InAppMessageTriggerDeterminer
) : EventListener {

    private val currentState: AppState
        get() = appStateManager.currentState

    override fun onEventPublish(userEvent: UserEvent) {
        val workspace = workspaceFetcher.fetch() ?: return

        val inAppMessageRenderSource =
            inAppMessageTriggerDeterminer.determine(workspace.inAppMessages, userEvent, workspace)
                ?: return

        if (currentState == AppState.FOREGROUND) {
            inAppMessageRenderer.render(
                inAppMessageRenderSource
            )
        }
    }
}