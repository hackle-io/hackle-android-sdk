package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleListener
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.DELAYED
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.TimeUnit

internal class InAppMessageDelayScheduler(
    private val clock: Clock,
    private val scheduler: Scheduler,
) {

    lateinit var listener: InAppMessageScheduleListener

    fun schedule(delay: InAppMessageDelay): InAppMessageDelayTask {
        val command = DelayCommand(delay)
        val job = scheduler.schedule(delay.delayMillis, TimeUnit.MILLISECONDS, command)
        return ScheduledInAppMessageDelayTask(delay, job)
    }

    private inner class DelayCommand(private val delay: InAppMessageDelay) : () -> Unit {
        override fun invoke() {
            val now = clock.currentMillis()
            val request = delay.schedule.toRequest(DELAYED, now)
            listener.onSchedule(request)
        }
    }

    private class ScheduledInAppMessageDelayTask(
        override val delay: InAppMessageDelay,
        private val job: ScheduledJob,
    ) : InAppMessageDelayTask {
        override val isCompleted: Boolean get() = job.isCompleted
        override fun cancel() {
            job.cancel()
            log.debug { "InAppMessage Delay cancelled: $delay" }
        }
    }

    companion object {
        private val log = Logger<InAppMessageDelayScheduler>()
    }
}
