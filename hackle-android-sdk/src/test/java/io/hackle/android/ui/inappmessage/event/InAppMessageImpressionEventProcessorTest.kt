package io.hackle.android.ui.inappmessage.event

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageImpressionEventProcessorTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageImpressionEventProcessor()
        expectThat(sut.supports(InAppMessageEvent.Impression)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Close)).isFalse()
    }
}
