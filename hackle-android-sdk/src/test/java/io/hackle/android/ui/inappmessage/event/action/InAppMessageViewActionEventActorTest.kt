package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.DefaultInAppMessageListener
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvents
import io.hackle.android.ui.inappmessage.view.InAppMessageView
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

class InAppMessageViewActionEventActorTest {

    @MockK
    private lateinit var handlerFactory: InAppMessageActionHandlerFactory

    @MockK
    private lateinit var handler: InAppMessageActionHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageViewActionEventActor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { handlerFactory.get(any()) } returns handler
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageViewEvents.action())).isTrue()
        expectThat(sut.supports(InAppMessageViewEvents.impression())).isFalse()
    }

    @Test
    fun `process - when custom processed then do not handle action`() {
        // given
        val listener = mockk<HackleInAppMessageListener>()
        every { listener.onInAppMessageClick(any(), any(), any()) } returns true

        val view = view(listener, InAppMessageView.State.OPENED)
        val event = InAppMessageViewEvents.action()

        // when
        sut.action(view, event)

        // then
        verify(exactly = 1) {
            listener.onInAppMessageClick(any(), any(), any())
        }
        verify(exactly = 0) {
            handler.handle(any(), any())
        }
    }

    @Test
    fun `process - when view is closed then do not handle action`() {
        // given
        val listener = mockk<HackleInAppMessageListener>()
        every { listener.onInAppMessageClick(any(), any(), any()) } returns false

        val view = view(listener, InAppMessageView.State.CLOSED)
        val event = InAppMessageViewEvents.action()

        // when
        sut.action(view, event)

        // then
        verify(exactly = 0) {
            handler.handle(any(), any())
        }
    }

    @Test
    fun `process - when cannot found handler then do nothing`() {
        // given
        val view = view(DefaultInAppMessageListener, InAppMessageView.State.OPENED)
        val event = InAppMessageViewEvents.action()
        every { handlerFactory.get(any()) } returns null

        // when
        sut.action(view, event)
    }

    @Test
    fun `process - handle`() {
        // given
        val view = view(DefaultInAppMessageListener, InAppMessageView.State.OPENED)
        val event = InAppMessageViewEvents.action()

        // when
        sut.action(view, event)

        // then
        verify(exactly = 1) {
            handler.handle(view, any())
        }
    }

    private fun view(listener: HackleInAppMessageListener, state: InAppMessageView.State): InAppMessageView {
        return mockk(relaxed = true) {
            every { this@mockk.state } returns state
            every { controller } returns mockk {
                every { ui } returns mockk {
                    every { this@mockk.listener } returns listener
                }
            }
        }
    }
}
