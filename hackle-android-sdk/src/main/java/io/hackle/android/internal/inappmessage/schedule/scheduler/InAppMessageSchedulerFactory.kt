package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType

internal class InAppMessageSchedulerFactory(
    private val schedulers: List<InAppMessageScheduler>,
) {

    fun get(scheduleType: InAppMessageScheduleType): InAppMessageScheduler {
        val scheduler = schedulers.find { it.supports(scheduleType) }
        return requireNotNull(scheduler) { "Unsupported InAppMessageScheduleType[$scheduleType]" }
    }
}
