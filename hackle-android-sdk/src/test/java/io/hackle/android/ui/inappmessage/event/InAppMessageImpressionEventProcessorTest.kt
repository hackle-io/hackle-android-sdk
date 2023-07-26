package io.hackle.android.ui.inappmessage.event

import io.hackle.android.internal.database.MapKeyValueRepository
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpressionStorage
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.assertions.last

internal class InAppMessageImpressionEventProcessorTest {

    private lateinit var impressionStorage: InAppMessageImpressionStorage
    private lateinit var sut: InAppMessageImpressionEventProcessor

    @Before
    fun before() {
        impressionStorage = InAppMessageImpressionStorage(MapKeyValueRepository())
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
        val view = mockk<InAppMessageView>(relaxUnitFun = true) {
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
        val view = mockk<InAppMessageView>(relaxUnitFun = true) {
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
}
