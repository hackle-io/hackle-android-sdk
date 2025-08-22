package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.ConcurrentMap

internal class InAppMessageDelayManager(
    private val scheduler: InAppMessageDelayScheduler,
    private val tasks: ConcurrentMap<String, InAppMessageDelayTask>,
) {

    fun registerAndDelay(request: InAppMessageScheduleRequest): InAppMessageDelay {
        // App SDK only delays without register.
        return delay(request)
    }

    fun delay(request: InAppMessageScheduleRequest): InAppMessageDelay {
        ensureDelay(request)

        val delay = InAppMessageDelay.from(request)
        val task = scheduler.schedule(delay)
        tasks[delay.schedule.dispatchId] = task

        log.debug { "InAppMessage Delay started: dispatchId=${request.schedule.dispatchId}" }
        return delay
    }

    private fun ensureDelay(request: InAppMessageScheduleRequest) {
        val existing = tasks[request.schedule.dispatchId] ?: return
        require(existing.isCompleted) { "Existing delay is not completed: $request" }
        tasks.remove(request.schedule.dispatchId)
    }

    fun delete(request: InAppMessageScheduleRequest): InAppMessageDelay? {
        val delay = tasks.remove(request.schedule.dispatchId)?.delay
        if (delay != null) {
            log.debug { "InAppMessage Delay removed: dispatchId=${request.schedule.dispatchId}" }
        }
        return delay
    }

    fun cancelAll(): List<InAppMessageDelay> {
        val snapshot = tasks.values.toList()
        tasks.clear()
        snapshot.forEach { it.cancel() }
        return snapshot.map { it.delay }
    }

    companion object {
        private val log = Logger<InAppMessageDelayManager>()
    }
}
