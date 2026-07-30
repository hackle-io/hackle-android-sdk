package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleActionDeterminer
import io.hackle.android.internal.inappmessage.schedule.scheduler.InAppMessageSchedulerFactory
import io.hackle.android.internal.task.Futures
import io.hackle.android.internal.task.onSuccess
import io.hackle.android.internal.task.recover
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CompletableFuture

internal class InAppMessageScheduleProcessor(
    private val actionDeterminer: InAppMessageScheduleActionDeterminer,
    private val schedulerFactory: InAppMessageSchedulerFactory,
) : InAppMessageScheduleListener {

    fun process(request: InAppMessageScheduleRequest): CompletableFuture<InAppMessageScheduleResponse> {
        log.debug { "InAppMessage Schedule Request: $request" }
        return Futures.wrap { schedule(request) }
            .recover {
                log.error { "Failed to process InAppMessageSchedule: $it" }
                InAppMessageScheduleResponse.of(request, Code.EXCEPTION)
            }
            .onSuccess {
                log.debug { "InAppMessage Schedule Response: $it" }
            }
    }

    private fun schedule(request: InAppMessageScheduleRequest): CompletableFuture<InAppMessageScheduleResponse> {
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
