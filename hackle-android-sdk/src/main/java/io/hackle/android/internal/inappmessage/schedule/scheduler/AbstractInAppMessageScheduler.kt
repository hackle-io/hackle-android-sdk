package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleAction
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleAction.*

internal abstract class AbstractInAppMessageScheduler : InAppMessageScheduler {

    protected abstract fun deliver(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse
    protected abstract fun delay(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse
    protected abstract fun ignore(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse

    final override fun schedule(
        action: InAppMessageScheduleAction,
        request: InAppMessageScheduleRequest,
    ): InAppMessageScheduleResponse {
        return when (action) {
            DELIVER -> deliver(request)
            DELAY -> delay(request)
            IGNORE -> ignore(request)
        }
    }
}
