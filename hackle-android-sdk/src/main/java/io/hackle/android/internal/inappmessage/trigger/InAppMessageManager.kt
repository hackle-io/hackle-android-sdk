package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEventListener
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresenter
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateProvider
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageManager(
    private val determiner: InAppMessageDeterminer,
    private val presenter: InAppMessagePresenter,
    private val appStateProvider: AppStateProvider,
) : UserEventListener {

    override fun onEvent(event: UserEvent) {
        val context = determine(event) ?: return

        if (appStateProvider.currentState != AppState.FOREGROUND) {
            return
        }
        presenter.present(context)
    }

    private fun determine(event: UserEvent): InAppMessagePresentationContext? {
        return try {
            determiner.determineOrNull(event)
        } catch (e: Exception) {
            log.error { "Failed to determine InAppMessage: $e" }
            null
        }
    }

    companion object {
        private val log = Logger<InAppMessageManager>()
    }
}
