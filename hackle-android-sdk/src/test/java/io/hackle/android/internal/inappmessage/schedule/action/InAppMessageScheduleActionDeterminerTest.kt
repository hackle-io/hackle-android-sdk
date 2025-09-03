package io.hackle.android.internal.inappmessage.schedule.action

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageScheduleActionDeterminerTest {

    private val sut = InAppMessageScheduleActionDeterminer()

    @Test
    fun `determine`() {
        expectThat(sut.determine(request(1))) isEqualTo InAppMessageScheduleAction.DELAY
        expectThat(sut.determine(request(0))) isEqualTo InAppMessageScheduleAction.DELIVER
        expectThat(sut.determine(request(-60000))) isEqualTo InAppMessageScheduleAction.DELIVER
        expectThat(sut.determine(request(-60001))) isEqualTo InAppMessageScheduleAction.IGNORE
    }

    private fun request(delayMillis: Long): InAppMessageScheduleRequest {
        return mockk(relaxed = true) {
            every { this@mockk.delayMillis } returns delayMillis
        }
    }
}