package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest

internal data class InAppMessageDelay(
    val schedule: InAppMessageSchedule,
    val requestedAt: Long,
) {
    val delayMillis: Long get() = schedule.time.delayMillis(requestedAt)

    override fun toString(): String {
        return "InAppMessageDelay(dispatchId=${schedule.dispatchId}, inAppMessageKey=${schedule.inAppMessageKey}, delayMillis=${delayMillis}ms, requestedAt=$requestedAt, deliverAt=${schedule.time.deliverAt})"
    }

    companion object {
        fun from(request: InAppMessageScheduleRequest): InAppMessageDelay {
            return InAppMessageDelay(
                schedule = request.schedule,
                requestedAt = request.requestedAt
            )
        }
    }
}
