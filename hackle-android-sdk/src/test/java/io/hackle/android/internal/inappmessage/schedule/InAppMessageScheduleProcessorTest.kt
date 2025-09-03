package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleAction
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleActionDeterminer
import io.hackle.android.internal.inappmessage.schedule.scheduler.InAppMessageScheduler
import io.hackle.android.internal.inappmessage.schedule.scheduler.InAppMessageSchedulerFactory
import io.hackle.android.support.InAppMessages
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageScheduleProcessorTest {

    @MockK
    private lateinit var actionDeterminer: InAppMessageScheduleActionDeterminer

    @MockK
    private lateinit var schedulerFactory: InAppMessageSchedulerFactory

    @MockK
    private lateinit var scheduler: InAppMessageScheduler

    @InjectMockKs
    private lateinit var sut: InAppMessageScheduleProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { schedulerFactory.get(any()) } returns scheduler
    }

    @Test
    fun `schedule`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.TRIGGERED, 42)
        val response = InAppMessageScheduleResponse.of(request, Code.DELIVER, null, null)
        every { scheduler.schedule(any(), any()) } returns response
        every { actionDeterminer.determine(any()) } returns InAppMessageScheduleAction.DELIVER

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo response
        verify(exactly = 1) {
            scheduler.schedule(InAppMessageScheduleAction.DELIVER, request)
        }
    }

    @Test
    fun `exception in schedule`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.TRIGGERED, 42)
        every { scheduler.schedule(any(), any()) } throws IllegalArgumentException("fail")
        every { actionDeterminer.determine(any()) } returns InAppMessageScheduleAction.DELIVER

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageScheduleResponse.of(request, Code.EXCEPTION, null, null)
    }

    @Test
    fun `onSchedule`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.TRIGGERED, 42)
        val response = InAppMessageScheduleResponse.of(request, Code.DELIVER, null, null)
        every { scheduler.schedule(any(), any()) } returns response
        every { actionDeterminer.determine(any()) } returns InAppMessageScheduleAction.DELIVER

        // when
        sut.onSchedule(request)

        // then
        verify(exactly = 1) {
            scheduler.schedule(InAppMessageScheduleAction.DELIVER, request)
        }
    }
}
