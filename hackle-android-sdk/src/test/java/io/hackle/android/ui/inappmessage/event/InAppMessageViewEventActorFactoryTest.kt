package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageViewEventActor
import io.hackle.android.ui.inappmessage.event.action.InAppMessageViewEventActorFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull


internal class InAppMessageViewEventActorFactoryTest {

    @Test
    fun `empty`() {
        val sut = InAppMessageViewEventActorFactory(listOf())
        expectThat(sut.get(InAppMessageViewEvents.impression())).isNull()
    }

    @Test
    fun `find first supported processor`() {
        val processor = mockk<InAppMessageViewEventActor<InAppMessageViewEvent>>()
        every { processor.supports(any()) } returnsMany listOf(false, false, true, false)

        val sut = InAppMessageViewEventActorFactory(listOf(processor, processor, processor, processor))

        expectThat(sut.get(mockk())).isNotNull()
        verify(exactly = 3) {
            processor.supports(any())
        }
    }
}
