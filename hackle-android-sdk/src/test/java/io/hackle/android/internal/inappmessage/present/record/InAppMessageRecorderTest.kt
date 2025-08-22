package io.hackle.android.internal.inappmessage.present.record

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageImpressionStorage
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.target.InAppMessageImpressionStorage
import io.hackle.sdk.core.user.HackleUser
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class InAppMessageRecorderTest {

    private lateinit var impressionStorage: InAppMessageImpressionStorage
    private lateinit var sut: InAppMessageRecorder

    @Before
    fun before() {
        impressionStorage = AndroidInAppMessageImpressionStorage(MapKeyValueRepository())
        sut = InAppMessageRecorder(impressionStorage)
    }

    @Test
    fun `process - save impression`() {

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

        val impressions = impressionStorage.get(inAppMessage)
        expectThat(impressions) {
            hasSize(1)
            get { first() }.and {
                get { identifiers } isEqualTo mapOf("a" to "1", "b" to "2")
                get { timestamp } isEqualTo 320L
            }
        }
    }

    @Test
    fun `process - when exceed impression limit then remove first`() {
        val inAppMessage = InAppMessages.create(id = 42)

        repeat(100) {
            val request = InAppMessages.presentRequest(
                inAppMessage = inAppMessage,
                requestedAt = it.toLong()
            )

            sut.record(request, mockk())
        }

        expectThat(impressionStorage.get(inAppMessage)).hasSize(100)

        val request = InAppMessages.presentRequest(
            inAppMessage = inAppMessage,
            requestedAt = 320
        )
        sut.record(request, mockk())

        expectThat(impressionStorage.get(inAppMessage)) {
            hasSize(100)
            get { first().timestamp } isEqualTo 1
            get { last().timestamp } isEqualTo 320
        }
    }

    @Test
    fun `process - override`() {
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.create(id = 42)

        val request = InAppMessages.presentRequest(
            inAppMessage = inAppMessage,
            requestedAt = 320L,
            evaluation = InAppMessageEvaluation(true, DecisionReason.OVERRIDDEN)
        )

        sut.record(request, mockk())

        val impressions = impressionStorage.get(inAppMessage)
        expectThat(impressions) {
            hasSize(0)
        }
    }
}
