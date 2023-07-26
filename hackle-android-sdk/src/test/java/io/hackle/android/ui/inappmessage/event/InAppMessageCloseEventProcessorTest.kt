package io.hackle.android.ui.inappmessage.event

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageCloseEventProcessorTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageCloseEventProcessor()
        expectThat(sut.supports(InAppMessageEvent.Close)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Impression)).isFalse()
    }
}
