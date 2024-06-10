package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEventListener
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.presentation.InAppMessagePresenter
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.ActivityState
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageManager(
    private val determiner: InAppMessageDeterminer,
    private val presenter: InAppMessagePresenter,
    private val activityProvider: ActivityProvider,
) : UserEventListener {

    override fun onEvent(event: UserEvent) {
        val context = determine(event) ?: return

        if (activityProvider.currentState != ActivityState.ACTIVE) {
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
