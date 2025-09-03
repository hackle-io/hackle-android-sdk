package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.support.InAppMessages
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageScheduleRequestTest {

    @Test
    fun `instance`() {
        val schedule = InAppMessages.schedule(time = InAppMessageSchedule.Time(1001, 2000))
        val request = schedule.toRequest(InAppMessageScheduleType.TRIGGERED, 1500)

        expectThat(request) {
            get { this.schedule } isEqualTo schedule
            get { this.scheduleType } isEqualTo InAppMessageScheduleType.TRIGGERED
            get { this.delayMillis } isEqualTo 500L
        }
    }
}
