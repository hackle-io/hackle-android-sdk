package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageEventActor
import io.hackle.android.ui.inappmessage.event.action.InAppMessageEventActorFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull


internal class InAppMessageEventActorFactoryTest {

    @Test
    fun `empty`() {
        val sut = InAppMessageEventActorFactory(listOf())
        expectThat(sut.get(InAppMessageEvent.Impression)).isNull()
    }

    @Test
    fun `find first supported processor`() {
        val processor = mockk<InAppMessageEventActor<InAppMessageEvent>>()
        every { processor.supports(any()) } returnsMany listOf(false, false, true, false)

        val sut = InAppMessageEventActorFactory(listOf(processor, processor, processor, processor))

        expectThat(sut.get(mockk())).isNotNull()
        verify(exactly = 3) {
            processor.supports(any())
        }
    }
}
