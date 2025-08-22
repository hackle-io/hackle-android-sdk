package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleListener
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.internal.time.FixedClock
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InAppMessageDelaySchedulerTest {

    @Test
    fun `delay trigger`() {
        // given
        val clock = FixedClock(2001)
        val scheduler = mockk<Scheduler>()
        val listener = mockk<InAppMessageScheduleListener>(relaxed = true)

        val sut = InAppMessageDelayScheduler(clock, scheduler)
        sut.listener = listener

        val job = mockk<ScheduledJob>()
        every { scheduler.schedule(any(), any(), any()) } answers {
            thirdArg<() -> Unit>().invoke()
            job
        }

        val schedule = InAppMessages.schedule(
            time = InAppMessageSchedule.Time(1001, 2000)
        )
        val delay = InAppMessageDelay(schedule, 1500)

        // when
        val task = sut.schedule(delay)

        // then
        expectThat(task.delay) isEqualTo delay
        verify(exactly = 1) {
            scheduler.schedule(500, TimeUnit.MILLISECONDS, any())
        }
        verify(exactly = 1) {
            listener.onSchedule(withArg {
                expectThat(it) {
                    get { scheduleType } isEqualTo InAppMessageScheduleType.DELAYED
                    get { requestedAt } isEqualTo 2001L
                }
            })
        }
    }

    @Test
    fun `task complete`() {
        val sut =
            InAppMessageDelayScheduler(Clock.SYSTEM, Schedulers.executor(Executors.newSingleThreadScheduledExecutor()))

        val schedule = InAppMessages.schedule(
            time = InAppMessageSchedule.Time(1001, 2000)
        )
        val delay = InAppMessageDelay(schedule, 1950)

        val task = sut.schedule(delay)

        expectThat(task.isCompleted).isEqualTo(false)
        Thread.sleep(200)
        expectThat(task.isCompleted).isEqualTo(true)
    }

    @Test
    fun `task cancel`() {
        val sut =
            InAppMessageDelayScheduler(Clock.SYSTEM, Schedulers.executor(Executors.newSingleThreadScheduledExecutor()))

        val schedule = InAppMessages.schedule(
            time = InAppMessageSchedule.Time(1001, 2000)
        )
        val delay = InAppMessageDelay(schedule, 1000)

        val task = sut.schedule(delay)

        expectThat(task.isCompleted).isEqualTo(false)
        task.cancel()
        expectThat(task.isCompleted).isEqualTo(true)
    }
}
