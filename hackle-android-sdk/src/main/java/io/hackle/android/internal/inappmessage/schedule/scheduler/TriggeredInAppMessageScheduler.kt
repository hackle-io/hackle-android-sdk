package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType

internal class TriggeredInAppMessageScheduler(
    private val deliverProcessor: InAppMessageDeliverProcessor,
    private val delayManager: InAppMessageDelayManager,
) : AbstractInAppMessageScheduler() {
    override fun supports(scheduleType: InAppMessageScheduleType): Boolean {
        return scheduleType == InAppMessageScheduleType.TRIGGERED
    }

    override fun deliver(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val deliverRequest = InAppMessageDeliverRequest.of(request)
        val deliverResponse = deliverProcessor.process(deliverRequest)
        return InAppMessageScheduleResponse.of(request, Code.DELIVER, deliverResponse = deliverResponse)
    }

    override fun delay(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val delay = delayManager.registerAndDelay(request)
        return InAppMessageScheduleResponse.of(request, Code.DELAY, delay = delay)
    }

    override fun ignore(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        return InAppMessageScheduleResponse.of(request, Code.IGNORE)
    }
}
