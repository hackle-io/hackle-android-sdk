package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleAction

internal interface InAppMessageScheduler {
    fun supports(scheduleType: InAppMessageScheduleType): Boolean
    fun schedule(action: InAppMessageScheduleAction, request: InAppMessageScheduleRequest): InAppMessageScheduleResponse
}
