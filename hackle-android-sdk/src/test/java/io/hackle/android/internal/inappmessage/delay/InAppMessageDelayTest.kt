package io.hackle.android.internal.inappmessage.delay

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.support.InAppMessages
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageDelayTest {

    @Test
    fun `create`() {
        // given
        val schedule = InAppMessages.schedule(
            time = InAppMessageSchedule.Time(1001, 2000)
        )
        val delay = InAppMessageDelay(schedule, 1500)
        expectThat(delay) {
            get { delayMillis } isEqualTo 500L
        }
    }
}
