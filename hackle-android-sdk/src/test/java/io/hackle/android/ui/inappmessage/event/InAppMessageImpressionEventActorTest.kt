package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageImpressionViewEventActor
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageImpressionEventActorTest {

    private lateinit var sut: InAppMessageImpressionViewEventActor

    @Before
    fun before() {
        sut = InAppMessageImpressionViewEventActor()
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageViewEvent.Impression)).isTrue()
        expectThat(sut.supports(InAppMessageViewEvent.Close)).isFalse()
    }
}
