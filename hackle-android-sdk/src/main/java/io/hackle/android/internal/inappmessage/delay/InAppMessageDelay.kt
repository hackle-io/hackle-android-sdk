package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest

internal data class InAppMessageDelay(
    val schedule: InAppMessageSchedule,
    val requestedAt: Long,
) {
    val delayMillis: Long get() = schedule.time.delayMillis(requestedAt)

    companion object {
        fun from(request: InAppMessageScheduleRequest): InAppMessageDelay {
            return InAppMessageDelay(
                schedule = request.schedule,
                requestedAt = request.requestedAt
            )
        }
    }
}
