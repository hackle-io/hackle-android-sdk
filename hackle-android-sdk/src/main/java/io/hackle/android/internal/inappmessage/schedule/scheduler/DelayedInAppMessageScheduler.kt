package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType

internal class DelayedInAppMessageScheduler(
    private val deliverProcessor: InAppMessageDeliverProcessor,
    private val delayManager: InAppMessageDelayManager,
) : AbstractInAppMessageScheduler() {

    override fun supports(scheduleType: InAppMessageScheduleType): Boolean {
        return scheduleType == InAppMessageScheduleType.DELAYED
    }

    override fun deliver(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val delay = delayManager.delete(request)
        requireNotNull(delay) { "InAppMessageDelay not found (inAppMessageKey=${request.schedule.inAppMessageKey})" }

        val deliverRequest = InAppMessageDeliverRequest.of(request)
        val deliverResponse = deliverProcessor.process(deliverRequest)
        return InAppMessageScheduleResponse.of(request, Code.DELIVER, deliverResponse = deliverResponse)
    }

    override fun delay(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val delay = delayManager.delay(request)
        return InAppMessageScheduleResponse.of(request, Code.DELAY, delay = delay)
    }

    override fun ignore(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val delay = delayManager.delete(request)
        return InAppMessageScheduleResponse.of(request, Code.IGNORE, delay = delay)
    }
}
