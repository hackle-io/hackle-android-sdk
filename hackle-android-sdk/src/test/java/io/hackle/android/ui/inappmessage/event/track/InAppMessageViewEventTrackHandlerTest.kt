package io.hackle.android.ui.inappmessage.event.track

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class InAppMessageViewEventTrackHandlerTest {

    @RelaxedMockK
    private lateinit var tracker: InAppMessageEventTracker

    @InjectMockKs
    private lateinit var sut: InAppMessageViewEventTrackHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        for (handleType in InAppMessageViewEventHandleType.values()) {
            expectThat(sut.supports(handleType)).isEqualTo(handleType == InAppMessageViewEventHandleType.TRACK)
        }
    }

    @Test
    fun `track`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true)
        val event = mockk<InAppMessageViewEvent>()

        // when
        sut.handle(view, event)

        // then
        verify(exactly = 1) {
            tracker.track(any(), event)
        }
    }
}
