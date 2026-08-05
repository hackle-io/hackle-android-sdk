package io.hackle.android.internal.inappmessage.present.record

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse.Code
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.InMemoryInAppMessageImpressionStorage
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpressionStorage
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class InAppMessageRecorderTest {

    private lateinit var storage: InAppMessageImpressionStorage
    private lateinit var sut: InAppMessageRecorder

    @Before
    fun before() {
        storage = InMemoryInAppMessageImpressionStorage()
        sut = InAppMessageRecorder(storage)
    }

    @Test
    fun `record - PRESENT면 impression을 저장한다`() {
        // given
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.config(id = 42)

        val request = InAppMessages.presentRequest(
            user = user,
            inAppMessage = inAppMessage,
            requestedAt = 320L
        )
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context())

        // when
        sut.record(request, response)

        // then
        val impressions = storage.get(inAppMessage)
        expectThat(impressions) {
            hasSize(1)
            get { first() }.and {
                get { identifiers } isEqualTo mapOf("a" to "1", "b" to "2")
                get { timestamp } isEqualTo 320L
            }
        }
    }

    @Test
    fun `record - PRESENT가 아니면 impression을 저장하지 않는다`() {
        // given
        val inAppMessage = InAppMessages.config(id = 42)
        val request = InAppMessages.presentRequest(
            inAppMessage = inAppMessage,
            requestedAt = 320L
        )
        val response = InAppMessagePresentResponse.of(Code.ACTIVITY_NOT_FOUND, InAppMessages.context())

        // when
        sut.record(request, response)

        // then
        expectThat(storage.get(inAppMessage)).hasSize(0)
    }

    @Test
    fun `record - 저장 개수 제한을 넘으면 가장 오래된 impression부터 제거한다`() {
        // given
        val inAppMessage = InAppMessages.config(id = 42)
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context())

        repeat(100) {
            val request = InAppMessages.presentRequest(
                inAppMessage = inAppMessage,
                requestedAt = it.toLong()
            )
            sut.record(request, response)
        }
        expectThat(storage.get(inAppMessage)).hasSize(100)

        val request = InAppMessages.presentRequest(
            inAppMessage = inAppMessage,
            requestedAt = 320
        )

        // when
        sut.record(request, response)

        // then
        expectThat(storage.get(inAppMessage)) {
            hasSize(100)
            get { first().timestamp } isEqualTo 1
            get { last().timestamp } isEqualTo 320
        }
    }

    @Test
    fun `record - OVERRIDDEN이면 impression을 저장하지 않는다`() {
        // given
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.config(id = 42)

        val request = InAppMessages.presentRequest(
            user = user,
            inAppMessage = inAppMessage,
            requestedAt = 320L,
            reason = DecisionReason.OVERRIDDEN,
        )
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context())

        // when
        sut.record(request, response)

        // then
        expectThat(storage.get(inAppMessage)).hasSize(0)
    }

    @Test
    fun `record - 저장 중 예외가 발생해도 전파하지 않는다`() {
        // given
        val request = InAppMessages.presentRequest(
            inAppMessage = InAppMessages.config(id = 42),
            requestedAt = 320L
        )
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context())

        val failingStorage = mockk<InAppMessageImpressionStorage>(relaxed = true)
        every { failingStorage.get(any()) } throws IllegalArgumentException("fail")
        val sut = InAppMessageRecorder(failingStorage)

        // when
        sut.record(request, response)

        // then
        verify(exactly = 0) {
            failingStorage.set(any(), any())
        }
    }
}
