package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.internal.time.Clock

internal class InAppMessageEventHandler(
    private val clock: Clock,
    private val eventTracker: InAppMessageEventTracker,
    private val processorFactory: InAppMessageEventProcessorFactory
) {
    fun handle(view: InAppMessageView, event: InAppMessageEvent) {
        val timestamp = clock.currentMillis()
        eventTracker.track(view.context, event, timestamp)
        val processor = processorFactory.get(event) ?: return
        processor.process(view, event, timestamp)
    }
}
