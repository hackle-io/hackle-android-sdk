package io.hackle.android.ui.inappmessage.event

import io.hackle.android.ui.inappmessage.event.action.InAppMessageCloseEventActor
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageCloseEventActorTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageCloseEventActor()
        expectThat(sut.supports(InAppMessageEvent.Close)).isTrue()
        expectThat(sut.supports(InAppMessageEvent.Impression)).isFalse()
    }
}
