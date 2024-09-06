package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.DefaultInAppMessageListener
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.common.HackleInAppMessageListener
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
        val layout = layout(DefaultInAppMessageListener)
        val event = InAppMessageEvent.Action(mockk(), mockk())
        every { actionHandlerFactory.get(any()) } returns null

        // when
        sut.process(layout, event, 42)
    }

    @Test
    fun `process - handle`() {
        // given
        val layout = layout(DefaultInAppMessageListener)
        val event = InAppMessageEvent.Action(mockk(), mockk())

        val handler = mockk<InAppMessageActionHandler>(relaxUnitFun = true)
        every { actionHandlerFactory.get(any()) } returns handler

        // when
        sut.process(layout, event, 42)

        // then
        verify(exactly = 1) {
            handler.handle(layout, any())
        }
    }

    @Test
    fun `process - when custom processed then to not handle action`() {
        // given
        val listener = mockk<HackleInAppMessageListener>()
        every { listener.onInAppMessageClick(any(), any(), any()) } returns true

        val layout = layout(listener)
        val event = InAppMessageEvent.Action(mockk(), mockk())

        val handler = mockk<InAppMessageActionHandler>(relaxUnitFun = true)
        every { actionHandlerFactory.get(any()) } returns handler

        // when
        sut.process(layout, event, 42)

        // then
        verify(exactly = 1) {
            listener.onInAppMessageClick(any(), any(), any())
        }
        verify(exactly = 0) {
            handler.handle(any(), any())
        }
    }

    private fun layout(listener: HackleInAppMessageListener): InAppMessageLayout {
        return mockk(relaxed = true) {
            every { controller } returns mockk {
                every { ui } returns mockk {
                    every { this@mockk.listener } returns listener
                }
            }
        }
    }
}
