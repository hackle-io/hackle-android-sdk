package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.internal.task.Task

internal class TriggeredInAppMessageScheduler(
    private val deliverProcessor: InAppMessageDeliverProcessor,
    private val delayManager: InAppMessageDelayManager,
) : AbstractInAppMessageScheduler() {
    override fun supports(scheduleType: InAppMessageScheduleType): Boolean {
        return scheduleType == InAppMessageScheduleType.TRIGGERED
    }

    override fun deliver(request: InAppMessageScheduleRequest): Task<InAppMessageScheduleResponse> {
        val deliverRequest = InAppMessageDeliverRequest.of(request)
        return deliverProcessor.process(deliverRequest)
            .map { InAppMessageScheduleResponse.of(request, Code.DELIVER, deliverResponse = it) }
    }

    override fun delay(request: InAppMessageScheduleRequest): Task<InAppMessageScheduleResponse> {
        val delay = delayManager.registerAndDelay(request)
        val response = InAppMessageScheduleResponse.of(request, Code.DELAY, delay = delay)
        return Task.succeed(response)
    }

    override fun ignore(request: InAppMessageScheduleRequest): Task<InAppMessageScheduleResponse> {
        val response = InAppMessageScheduleResponse.of(request, Code.IGNORE)
        return Task.succeed(response)
    }
}
