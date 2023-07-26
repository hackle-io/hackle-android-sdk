package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageActionEventProcessorTest {

    @MockK
    private lateinit var actionHandlerFactory: InAppMessageActionHandlerFactory

    @InjectMockKs
    private lateinit var sut: InAppMessageActionEventProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageEvent.Action(mockk(), mockk()))).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Impression)).isFalse()
    }

    @Test
    fun `process - when cannot found handler then do nothing`() {
        // given
        val view = mockk<InAppMessageView>()
        val event = InAppMessageEvent.Action(mockk(), mockk())
        every { actionHandlerFactory.get(any()) } returns null

        // when
        sut.process(view, event, 42)
    }

    @Test
    fun `process - handle`() {
        // given
        val view = mockk<InAppMessageView>()
        val event = InAppMessageEvent.Action(mockk(), mockk())

        val handler = mockk<InAppMessageActionHandler>(relaxUnitFun = true)
        every { actionHandlerFactory.get(any()) } returns handler

        // when
        sut.process(view, event, 42)

        // then
        verify(exactly = 1) {
            handler.handle(view, any())
        }
    }
}
