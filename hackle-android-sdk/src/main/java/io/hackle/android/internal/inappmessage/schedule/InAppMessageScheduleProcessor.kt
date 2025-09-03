package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleActionDeterminer
import io.hackle.android.internal.inappmessage.schedule.scheduler.InAppMessageSchedulerFactory
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageScheduleProcessor(
    private val actionDeterminer: InAppMessageScheduleActionDeterminer,
    private val schedulerFactory: InAppMessageSchedulerFactory,
) : InAppMessageScheduleListener {

    fun process(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        log.debug { "InAppMessage Schedule Request: $request" }

        val response = try {
            schedule(request)
        } catch (e: Exception) {
            log.error { "Failed to process InAppMessageSchedule: $e" }
            InAppMessageScheduleResponse.of(request, Code.EXCEPTION)
        }

        log.debug { "InAppMessage Schedule Response: $response" }
        return response
    }

    private fun schedule(request: InAppMessageScheduleRequest): InAppMessageScheduleResponse {
        val action = actionDeterminer.determine(request)
        val scheduler = schedulerFactory.get(request.scheduleType)
        return scheduler.schedule(action, request)
    }

    override fun onSchedule(request: InAppMessageScheduleRequest) {
        process(request)
    }

    companion object {
        private val log = Logger<InAppMessageScheduleProcessor>()
    }
}
