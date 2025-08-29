package io.hackle.android.internal.inappmessage.present.record

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageImpressionStorage
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.target.InAppMessageImpressionStorage
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
        storage = AndroidInAppMessageImpressionStorage(MapKeyValueRepository())
        sut = InAppMessageRecorder(storage)
    }

    @Test
    fun `record - save impression`() {

        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.create(id = 42)

        val request = InAppMessages.presentRequest(
            user = user,
            inAppMessage = inAppMessage,
            requestedAt = 320L
        )

        sut.record(request, mockk())

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
    fun `record - when exceed impression limit then remove first`() {
        val inAppMessage = InAppMessages.create(id = 42)

        repeat(100) {
            val request = InAppMessages.presentRequest(
                inAppMessage = inAppMessage,
                requestedAt = it.toLong()
            )

            sut.record(request, mockk())
        }

        expectThat(storage.get(inAppMessage)).hasSize(100)

        val request = InAppMessages.presentRequest(
            inAppMessage = inAppMessage,
            requestedAt = 320
        )
        sut.record(request, mockk())

        expectThat(storage.get(inAppMessage)) {
            hasSize(100)
            get { first().timestamp } isEqualTo 1
            get { last().timestamp } isEqualTo 320
        }
    }

    @Test
    fun `record - override`() {
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.create(id = 42)

        val request = InAppMessages.presentRequest(
            user = user,
            inAppMessage = inAppMessage,
            requestedAt = 320L,
            reason = DecisionReason.OVERRIDDEN,
        )

        sut.record(request, mockk())

        val impressions = storage.get(inAppMessage)
        expectThat(impressions) {
            hasSize(0)
        }
    }

    @Test
    fun `record - exception`() {
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.create(id = 42)

        val request = InAppMessages.presentRequest(
            user = user,
            inAppMessage = inAppMessage,
            requestedAt = 320L
        )

        val storage = mockk<InAppMessageImpressionStorage>(relaxed = true)
        every { storage.get(any()) } throws IllegalArgumentException("fail")

        val sut = InAppMessageRecorder(storage)

        sut.record(request, mockk())

        verify(exactly = 0) {
            storage.set(any(), any())
        }
    }
}
