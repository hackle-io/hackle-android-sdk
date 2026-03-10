package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.inAppMessage
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock

internal class InAppMessageEventHandler(
    private val clock: Clock,
    private val eventTracker: InAppMessageEventTracker,
    private val processorFactory: InAppMessageEventProcessorFactory,
) {
    fun handle(view: InAppMessageView, event: InAppMessageEvent) {
        log.debug { "InAppMessage Handle: dispatchId=${view.presentationContext.dispatchId}, inAppMessageKey=${view.inAppMessage.key}, event=${event}" }

        val timestamp = clock.currentMillis()
        eventTracker.track(view.presentationContext, event, timestamp)
        val processor = processorFactory.get(event) ?: return
        processor.process(view, event, timestamp)
    }

    companion object {
        private val log = Logger<InAppMessageEventHandler>()
    }
}
