package io.hackle.android.internal.inappmessage.trigger

import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageTriggerProcessor(
    private val determiner: InAppMessageTriggerDeterminer,
    private val handler: InAppMessageTriggerHandler,
) {
    fun process(event: UserEvent) {
        try {
            val trigger = determiner.determine(event) ?: return
            log.debug { "InAppMessage triggered: $trigger" }

            handler.handle(trigger)
        } catch (e: Exception) {
            log.error { "Failed to process InAppMessage event trigger: $e" }
        }
    }

    companion object {
        private val log = Logger<InAppMessageTriggerProcessor>()
    }
}
