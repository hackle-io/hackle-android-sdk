package io.hackle.android.ui.inappmessage.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull


internal class InAppMessageEventProcessorFactoryTest {

    @Test
    fun `empty`() {
        val sut = InAppMessageEventProcessorFactory(listOf())
        expectThat(sut.get(InAppMessageEvent.Impression)).isNull()
    }

    @Test
    fun `find first supported processor`() {
        val processor = mockk<InAppMessageEventProcessor<InAppMessageEvent>>()
        every { processor.supports(any()) } returnsMany listOf(false, false, true, false)

        val sut = InAppMessageEventProcessorFactory(listOf(processor, processor, processor, processor))

        expectThat(sut.get(mockk())).isNotNull()
        verify(exactly = 3) {
            processor.supports(any())
        }
    }
}
