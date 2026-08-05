package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.TRIGGERED
import io.hackle.android.support.InAppMessages
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class InAppMessageDeliverResponseTest {

    @Test
    fun `of - request 정보와 code로 응답을 생성한다`() {
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(dispatchId = "111", inAppMessageKey = 320).toRequest(TRIGGERED, 42)
        )

        val response = InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)

        expectThat(response) {
            get { dispatchId } isEqualTo "111"
            get { inAppMessageKey } isEqualTo 320L
            get { code } isEqualTo Code.INELIGIBLE
            get { presentResponse }.isNull()
        }
    }

    @Test
    fun `of - presentResponse를 포함해 응답을 생성한다`() {
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(dispatchId = "111", inAppMessageKey = 320).toRequest(TRIGGERED, 42)
        )
        val present = mockk<InAppMessagePresentResponse>()

        val response = InAppMessageDeliverResponse.of(request, Code.DELIVER, present)

        expectThat(response) {
            get { dispatchId } isEqualTo "111"
            get { inAppMessageKey } isEqualTo 320L
            get { code } isEqualTo Code.DELIVER
            get { presentResponse } isSameInstanceAs present
        }
    }
}
