package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.ACTION
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.TRACK
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

internal class InAppMessageViewEventHandleProcessorTest {

    @MockK
    private lateinit var handlerFactory: InAppMessageViewEventHandlerFactory

    @MockK
    private lateinit var handler: InAppMessageViewEventHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageViewEventHandleProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { handlerFactory.get(any()) } returns handler
    }

    @Test
    fun `handle the event with given types`() {
        // given
        val view = mockk<InAppMessageView>()
        val event = mockk<InAppMessageViewEvent>()

        // when
        sut.process(view, event, listOf(TRACK, ACTION))

        // then
        verifySequence {
            handlerFactory.get(TRACK)
            handlerFactory.get(ACTION)
        }
        verify(exactly = 2) {
            handler.handle(view, event)
        }
    }

    @Test
    fun `when types is empty then do nothing`() {
        // given
        val view = mockk<InAppMessageView>()
        val event = mockk<InAppMessageViewEvent>()

        // when
        sut.process(view, event, listOf())

        // then
        verify(exactly = 0) {
            handler.handle(view, event)
        }
    }
}
