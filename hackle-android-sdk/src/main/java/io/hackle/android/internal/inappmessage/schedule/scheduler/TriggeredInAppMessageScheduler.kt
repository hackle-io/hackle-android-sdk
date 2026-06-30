package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.task.map
import java.util.concurrent.CompletableFuture

internal class TriggeredInAppMessageScheduler(
    private val deliverProcessor: InAppMessageDeliverProcessor,
    private val delayManager: InAppMessageDelayManager,
) : AbstractInAppMessageScheduler() {
    override fun supports(scheduleType: InAppMessageScheduleType): Boolean {
        return scheduleType == InAppMessageScheduleType.TRIGGERED
    }

    override fun deliver(request: InAppMessageScheduleRequest): CompletableFuture<InAppMessageScheduleResponse> {
        val deliverRequest = InAppMessageDeliverRequest.of(request)
        return deliverProcessor.process(deliverRequest)
            .map { InAppMessageScheduleResponse.of(request, Code.DELIVER, deliverResponse = it) }
    }

    override fun delay(request: InAppMessageScheduleRequest): CompletableFuture<InAppMessageScheduleResponse> {
        val delay = delayManager.registerAndDelay(request)
        val response = InAppMessageScheduleResponse.of(request, Code.DELAY, delay = delay)
        return response.asFuture()
    }

    override fun ignore(request: InAppMessageScheduleRequest): CompletableFuture<InAppMessageScheduleResponse> {
        val response = InAppMessageScheduleResponse.of(request, Code.IGNORE)
        return response.asFuture()
    }
}
