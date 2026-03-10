package io.hackle.android.ui.inappmessage.event

import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.event.action.InAppMessageEventActor
import io.hackle.android.ui.inappmessage.event.action.InAppMessageEventActorFactory
import io.hackle.android.ui.inappmessage.event.track.InAppMessageEventTracker
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test


internal class InAppMessageEventHandleProcessorTest {

    @MockK
    private lateinit var clock: Clock

    @RelaxedMockK
    private lateinit var eventTracker: InAppMessageEventTracker

    @MockK
    private lateinit var processorFactory: InAppMessageEventActorFactory

    @InjectMockKs
    private lateinit var sut: InAppMessageEventHandleProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { clock.currentMillis() } returns 42
        every { processorFactory.get(any()) } returns null
    }

    @Test
    fun `track`() {
        // given
        val context = InAppMessages.context()
        val view = mockk<InAppMessageView> {
            every { this@mockk.presentationContext } returns context
        }
        val event = InAppMessageEvent.Impression

        // when
        sut.handle(view, event)

        // then
        verify(exactly = 1) {
            eventTracker.track(context, event, 42)
        }
    }

    @Test
    fun `when cannot found event processor the do not process`() {
        // given
        val context = InAppMessages.context()
        val view = mockk<InAppMessageView> {
            every { this@mockk.presentationContext } returns context
        }
        val event = InAppMessageEvent.Impression

        every { processorFactory.get(any()) } returns null

        // when
        sut.handle(view, event)
    }

    @Test
    fun `process event`() {
        // given
        val context = InAppMessages.context()
        val view = mockk<InAppMessageView> {
            every { this@mockk.presentationContext } returns context
        }
        val event = InAppMessageEvent.Impression

        val eventProcessor = mockk<InAppMessageEventActor<InAppMessageEvent>>(relaxUnitFun = true)
        every { processorFactory.get(any()) } returns eventProcessor

        // when
        sut.handle(view, event)

        // then
        verify(exactly = 1) {
            eventProcessor.action(view, event, 42)
        }
    }
}
