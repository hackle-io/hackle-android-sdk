package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageViewEventActionHandlerTest {

    @MockK
    private lateinit var actorFactory: InAppMessageViewEventActorFactory

    @RelaxedMockK
    private lateinit var actor: InAppMessageViewEventActor<InAppMessageViewEvent>

    @InjectMockKs
    private lateinit var sut: InAppMessageViewEventActionHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { actorFactory.get(any()) } returns actor
    }

    @Test
    fun `supports`() {
        for (handleType in InAppMessageViewEventHandleType.values()) {
            expectThat(sut.supports(handleType)).isEqualTo(handleType == InAppMessageViewEventHandleType.ACTION)
        }
    }

    @Test
    fun `when not found actor for event then do nothing`() {
        // given
        every { actorFactory.get(any()) } returns null

        val view = mockk<InAppMessageView>()
        val event = mockk<InAppMessageViewEvent>()

        // when
        sut.handle(view, event)

        // then
        verify(exactly = 0) {
            actor.action(any(), any())
        }
    }

    @Test
    fun `handle with actor`() {
        // given
        val view = mockk<InAppMessageView>()
        val event = mockk<InAppMessageViewEvent>()

        // when
        sut.handle(view, event)

        // then
        verify(exactly = 1) {
            actor.action(view, event)
        }
    }
}
