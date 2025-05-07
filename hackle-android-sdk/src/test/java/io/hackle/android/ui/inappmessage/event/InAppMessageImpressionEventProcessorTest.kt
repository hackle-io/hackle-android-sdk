package io.hackle.android.ui.inappmessage.event

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageImpressionStorage
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageImpressionEventProcessorTest {

    private lateinit var impressionStorage: AndroidInAppMessageImpressionStorage
    private lateinit var sut: InAppMessageImpressionEventProcessor

    @Before
    fun before() {
        impressionStorage = AndroidInAppMessageImpressionStorage(MapKeyValueRepository())
        sut = InAppMessageImpressionEventProcessor(impressionStorage)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageEvent.Impression)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Close)).isFalse()
    }

    @Test
    fun `process - save impression`() {
        val user = HackleUser.builder()
            .identifier("a", "1")
            .identifier("b", "2")
            .build()
        val inAppMessage = InAppMessages.create(id = 42)
        val context = InAppMessages.context(inAppMessage = inAppMessage, user = user)
        val view = mockk<InAppMessageLayout>(relaxUnitFun = true) {
            every { this@mockk.context } returns context
        }

        sut.process(view, InAppMessageEvent.Impression, 320)

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
        val context = InAppMessages.context(inAppMessage = inAppMessage)
        val view = mockk<InAppMessageLayout>(relaxUnitFun = true) {
            every { this@mockk.context } returns context
        }

        repeat(100) {
            sut.process(view, InAppMessageEvent.Impression, it.toLong())
        }

        expectThat(impressionStorage.get(inAppMessage)).hasSize(100)
        sut.process(view, InAppMessageEvent.Impression, 320)
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
        val context = InAppMessages.context(inAppMessage = inAppMessage, user = user, decisionReason = DecisionReason.OVERRIDDEN)
        val view = mockk<InAppMessageLayout>(relaxUnitFun = true) {
            every { this@mockk.context } returns context
        }

        sut.process(view, InAppMessageEvent.Impression, 320)

        val impressions = impressionStorage.get(inAppMessage)
        expectThat(impressions) {
            hasSize(0)
        }
    }
}
