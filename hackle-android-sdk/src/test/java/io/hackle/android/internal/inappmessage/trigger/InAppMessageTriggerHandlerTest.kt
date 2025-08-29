package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleProcessor
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageTriggerHandlerTest {

    @RelaxedMockK
    private lateinit var scheduleProcessor: InAppMessageScheduleProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageTriggerHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `schedule`() {
        // given
        val inAppMessage = InAppMessages.create()
        val event = UserEvents.track("test", timestamp = 42)
        val trigger = InAppMessageTrigger(inAppMessage, DecisionReason.IN_APP_MESSAGE_TARGET, event)

        // when
        sut.handle(trigger)

        // then
        verify(exactly = 1) {
            scheduleProcessor.process(withArg {
                expectThat(it) {
                    get { scheduleType } isEqualTo InAppMessageScheduleType.TRIGGERED
                    get { requestedAt } isEqualTo 42L
                }
            })
        }
    }
}
