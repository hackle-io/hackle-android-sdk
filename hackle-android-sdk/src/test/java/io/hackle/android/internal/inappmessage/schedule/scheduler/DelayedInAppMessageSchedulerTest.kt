package io.hackle.android.internal.inappmessage.schedule.scheduler

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelay
import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleResponse.Code
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleAction
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.assertThrows
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DelayedInAppMessageSchedulerTest {

    @MockK
    private lateinit var deliverProcessor: InAppMessageDeliverProcessor

    @MockK
    private lateinit var delayManager: InAppMessageDelayManager

    @InjectMockKs
    private lateinit var sut: DelayedInAppMessageScheduler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageScheduleType.TRIGGERED)).isEqualTo(false)
        expectThat(sut.supports(InAppMessageScheduleType.DELAYED)).isEqualTo(true)
    }

    @Test
    fun `deliver`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.DELAYED, 42)
        val delay = mockk<InAppMessageDelay>()
        every { delayManager.delete(any()) } returns delay

        val deliverResponse = mockk<InAppMessageDeliverResponse>()
        every { deliverProcessor.process(any()) } returns deliverResponse

        // when
        val actual = sut.schedule(InAppMessageScheduleAction.DELIVER, request)

        // then
        expectThat(actual) isEqualTo InAppMessageScheduleResponse.of(
            request,
            Code.DELIVER,
            deliverResponse = deliverResponse
        )
    }

    @Test
    fun `deliver - delay not found`() {
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.DELAYED, 42)
        every { delayManager.delete(any()) } returns null

        assertThrows<IllegalArgumentException> {
            sut.schedule(InAppMessageScheduleAction.DELIVER, request)
        }
    }

    @Test
    fun `delay`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.DELAYED, 42)
        val delay = mockk<InAppMessageDelay>()
        every { delayManager.delay(any()) } returns delay

        // when
        val actual = sut.schedule(InAppMessageScheduleAction.DELAY, request)

        // then
        expectThat(actual) isEqualTo InAppMessageScheduleResponse.of(request, Code.DELAY, delay = delay)
    }

    @Test
    fun `ignore`() {
        // given
        val request = InAppMessages.schedule().toRequest(InAppMessageScheduleType.DELAYED, 42)
        val delay = mockk<InAppMessageDelay>()
        every { delayManager.delete(any()) } returns delay

        // when
        val actual = sut.schedule(InAppMessageScheduleAction.IGNORE, request)

        // then
        expectThat(actual) isEqualTo InAppMessageScheduleResponse.of(request, Code.IGNORE, delay = delay)
    }
}
