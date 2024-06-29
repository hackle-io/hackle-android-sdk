package io.hackle.android.ui.inappmessage.event

import io.hackle.android.internal.inappmessage.storage.InAppMessageImpression
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpressionStorage
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser

internal interface InAppMessageEventProcessor<EVENT : InAppMessageEvent> {
    fun supports(event: InAppMessageEvent): Boolean
    fun process(layout: InAppMessageLayout, event: EVENT, timestamp: Long)
}

internal class InAppMessageEventProcessorFactory(private val processors: List<InAppMessageEventProcessor<out InAppMessageEvent>>) {
    fun get(event: InAppMessageEvent): InAppMessageEventProcessor<InAppMessageEvent>? {
        @Suppress("UNCHECKED_CAST")
        return processors.find { it.supports(event) } as? InAppMessageEventProcessor<InAppMessageEvent>
    }
}

internal class InAppMessageImpressionEventProcessor(
    private val impressionStorage: InAppMessageImpressionStorage
) : InAppMessageEventProcessor<InAppMessageEvent.Impression> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Impression
    }

    override fun process(layout: InAppMessageLayout, event: InAppMessageEvent.Impression, timestamp: Long) {
        try {
            saveImpression(layout.context.inAppMessage, layout.context.user, timestamp)
        } catch (e: Throwable) {
            log.error { "Failed to process InAppMessageImpressionEvent: $e" }
        }
    }

    private fun saveImpression(inAppMessage: InAppMessage, user: HackleUser, timestamp: Long) {
        val impressions = impressionStorage.get(inAppMessage)
        val impression = InAppMessageImpression(user.identifiers, timestamp)

        val newImpressions = impressions + impression

        val impressionToSave = if (newImpressions.size > IMPRESSION_MAX_SIZE) {
            newImpressions.drop(newImpressions.size - IMPRESSION_MAX_SIZE)
        } else {
            newImpressions
        }
        impressionStorage.set(inAppMessage, impressionToSave)
    }

    companion object {
        private val log = Logger<InAppMessageImpressionEventProcessor>()
        private const val IMPRESSION_MAX_SIZE = 100
    }
}

internal class InAppMessageActionEventProcessor(
    private val actionHandlerFactory: InAppMessageActionHandlerFactory
) : InAppMessageEventProcessor<InAppMessageEvent.Action> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Action
    }

    override fun process(layout: InAppMessageLayout, event: InAppMessageEvent.Action, timestamp: Long) {
        val handler = actionHandlerFactory.get(event.action) ?: return
        handler.handle(layout, event.action)
    }
}

internal class InAppMessageCloseEventProcessor : InAppMessageEventProcessor<InAppMessageEvent.Close> {
    override fun supports(event: InAppMessageEvent): Boolean {
        return event is InAppMessageEvent.Close
    }

    override fun process(layout: InAppMessageLayout, event: InAppMessageEvent.Close, timestamp: Long) {
        // Do nothing. This method is called after the layout is closed.
    }
}
