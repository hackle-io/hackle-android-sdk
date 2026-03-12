package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvents
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageViewCloseEventActorTest {

    @InjectMockKs
    private lateinit var sut: InAppMessageViewCloseEventActor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        val sut = InAppMessageViewCloseEventActor()
        expectThat(sut.supports(InAppMessageViewEvents.close())).isTrue()
        expectThat(sut.supports(InAppMessageViewEvents.impression())).isFalse()
    }
}
