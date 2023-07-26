package io.hackle.android.ui.inappmessage.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull


internal class InAppMessageActionHandlerFactoryTest {

    @Test
    fun `when handlers are empty then returns null`() {
        val sut = InAppMessageActionHandlerFactory(listOf())
        expectThat(sut.get(mockk())).isNull()
    }

    @Test
    fun `find first supported handler`() {
        val handler = mockk<InAppMessageActionHandler>()
        every { handler.supports(any()) } returnsMany listOf(false, false, true, false)

        val sut = InAppMessageActionHandlerFactory(listOf(handler, handler, handler, handler))

        expectThat(sut.get(mockk())).isNotNull()
        verify(exactly = 3) { handler.supports(any()) }
    }
}
