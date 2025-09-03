package io.hackle.android.ui.inappmessage.event

import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageImpressionEventProcessorTest {

    private lateinit var sut: InAppMessageImpressionEventProcessor

    @Before
    fun before() {
        sut = InAppMessageImpressionEventProcessor()
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageEvent.Impression)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Close)).isFalse()
    }
}
