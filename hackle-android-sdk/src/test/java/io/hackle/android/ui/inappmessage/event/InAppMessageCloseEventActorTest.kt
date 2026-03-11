package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageCloseViewEventActor
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageCloseEventActorTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageCloseViewEventActor()
        expectThat(sut.supports(InAppMessageViewEvent.Close)).isTrue()
        expectThat(sut.supports(InAppMessageViewEvent.Impression)).isFalse()
    }
}
