package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.support.assertThrows
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class InAppMessageSchedulerFactoryTest {

    @Test
    fun `get first match`() {
        val scheduler1 = scheduler(false)
        val scheduler2 = scheduler(false)
        val scheduler3 = scheduler(true)
        val scheduler4 = scheduler(false)

        val sut = InAppMessageSchedulerFactory(listOf(scheduler1, scheduler2, scheduler3, scheduler4))

        val actual = sut.get(InAppMessageScheduleType.TRIGGERED)

        expectThat(actual) isSameInstanceAs scheduler3
        verify {
            scheduler4 wasNot Called
        }
    }

    @Test
    fun `not found`() {
        val scheduler1 = scheduler(false)
        val scheduler2 = scheduler(false)
        val scheduler3 = scheduler(false)
        val scheduler4 = scheduler(false)

        val sut = InAppMessageSchedulerFactory(listOf(scheduler1, scheduler2, scheduler3, scheduler4))

        assertThrows<IllegalArgumentException> {
            sut.get(InAppMessageScheduleType.TRIGGERED)
        }
    }

    private fun scheduler(supports: Boolean): InAppMessageScheduler {
        val scheduler = mockk<InAppMessageScheduler>()
        every { scheduler.supports(any()) } returns supports
        return scheduler
    }
}
