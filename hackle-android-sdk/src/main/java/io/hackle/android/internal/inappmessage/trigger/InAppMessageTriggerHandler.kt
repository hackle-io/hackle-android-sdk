package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleProcessor
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.TRIGGERED

internal class InAppMessageTriggerHandler(
    private val scheduleProcessor: InAppMessageScheduleProcessor,
) {
    fun handle(trigger: InAppMessageTrigger) {
        val schedule = InAppMessageSchedule.create(trigger)
        val scheduleRequest = schedule.toRequest(TRIGGERED, trigger.event.timestamp)
        scheduleProcessor.process(scheduleRequest)
    }
}
