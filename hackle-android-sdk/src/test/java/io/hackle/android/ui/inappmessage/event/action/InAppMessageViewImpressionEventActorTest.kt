package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvents
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageViewImpressionEventActorTest {

    private lateinit var sut: InAppMessageViewImpressionEventActor

    @Before
    fun before() {
        sut = InAppMessageViewImpressionEventActor()
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessageViewEvents.impression())).isTrue()
        expectThat(sut.supports(InAppMessageViewEvents.close())).isFalse()
    }
}
