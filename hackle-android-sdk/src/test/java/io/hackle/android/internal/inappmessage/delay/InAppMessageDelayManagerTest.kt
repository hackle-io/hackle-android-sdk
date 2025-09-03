package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.support.InAppMessages
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class InAppMessageDelayManagerTest {

    private lateinit var scheduler: InAppMessageDelayScheduler
    private lateinit var tasks: ConcurrentMap<String, InAppMessageDelayTask>

    private lateinit var sut: InAppMessageDelayManager

    @Before
    fun before() {
        scheduler = mockk()
        tasks = ConcurrentHashMap()

        sut = InAppMessageDelayManager(scheduler, tasks)
    }

    @Test
    fun `flow`() {
        val request1 = InAppMessages.schedule(dispatchId = "1").toRequest(InAppMessageScheduleType.TRIGGERED, 42)
        val task1 = task(request1)
        every { scheduler.schedule(any()) } returns task1

        // delay
        val delay = sut.delay(request1)
        expectThat(delay) {
            get { schedule } isEqualTo request1.schedule
        }
        expectThat(tasks) {
            hasSize(1)
            get { this["1"] } isEqualTo task1
        }

        // re delay
        sut.delay(request1)
        expectThat(tasks) {
            hasSize(1)
            get { this["1"] } isEqualTo task1
        }

        // delay 2
        val request2 = InAppMessages.schedule(dispatchId = "2").toRequest(InAppMessageScheduleType.TRIGGERED, 42)
        val task2 = task(request2)
        every { scheduler.schedule(any()) } returns task2

        sut.delay(request2)
        expectThat(tasks) {
            hasSize(2)
            get { this["2"] } isEqualTo task2
        }

        // delete 1
        val deletedDelay = sut.delete(request1)
        expectThat(deletedDelay).isNotNull()
            .get { schedule.dispatchId } isEqualTo "1"
        expectThat(tasks).hasSize(1)

        // cancelAll
        val cancelled = sut.cancelAll()
        expectThat(cancelled) {
            hasSize(1)
            first().get { schedule.dispatchId } isEqualTo "2"
        }
        expectThat(tasks).hasSize(0)
        expectThat(task2.cancelled).isEqualTo(true)
    }

    private fun task(request: InAppMessageScheduleRequest): MockInAppMessageDelayTask {
        return MockInAppMessageDelayTask(InAppMessageDelay.from(request))
    }

    private class MockInAppMessageDelayTask(
        override val delay: InAppMessageDelay,
    ) : InAppMessageDelayTask {
        var cancelled = false
        override fun cancel() {
            cancelled = true
        }
    }
}