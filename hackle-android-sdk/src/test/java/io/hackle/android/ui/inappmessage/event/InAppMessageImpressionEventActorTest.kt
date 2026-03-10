package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageImpressionEventActor
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageImpressionEventActorTest {

    private lateinit var sut: InAppMessageImpressionEventActor

    @Before
    fun before() {
        sut = InAppMessageImpressionEventActor()
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageEvent.Impression)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Close)).isFalse()
    }
}
