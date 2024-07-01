package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.internal.time.Clock

internal class InAppMessageEventHandler(
    private val clock: Clock,
    private val eventTracker: InAppMessageEventTracker,
    private val processorFactory: InAppMessageEventProcessorFactory
) {
    fun handle(layout: InAppMessageLayout, event: InAppMessageEvent) {
        val timestamp = clock.currentMillis()
        eventTracker.track(layout.context, event, timestamp)
        val processor = processorFactory.get(event) ?: return
        processor.process(layout, event, timestamp)
    }
}
