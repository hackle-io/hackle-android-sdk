package io.hackle.android.internal.inappmessage.schedule

internal data class InAppMessageScheduleRequest(
    val schedule: InAppMessageSchedule,
    val scheduleType: InAppMessageScheduleType,
    val requestedAt: Long,
) {

    val delayMillis: Long get() = schedule.time.deliverAt - requestedAt
}