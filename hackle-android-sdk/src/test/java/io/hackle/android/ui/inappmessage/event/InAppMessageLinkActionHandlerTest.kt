package io.hackle.android.ui.inappmessage.event

import android.app.Activity
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionType.WEB_LINK
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


internal class InAppMessageLinkActionHandlerTest {

    @RelaxedMockK
    private lateinit var uriHandler: UriHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageLinkActionHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessages.action(type = InAppMessage.ActionType.CLOSE))).isFalse()
        expectThat(sut.supports(InAppMessages.action(type = WEB_LINK))).isTrue()
    }

    @Test
    fun `handle - when activity is null then do nothing`() {
        // given
        val view = mockk<InAppMessageLayout> {
            every { activity } returns null
        }
        val action = InAppMessages.action(type = WEB_LINK)

        // when
        sut.handle(view, action)

        // then
        verify { uriHandler wasNot Called }
    }

    @Test
    fun `when action value is null then do nothing`() {
        // given
        val view = mockk<InAppMessageLayout> {
            every { activity } returns mockk()
        }
        val action = InAppMessages.action(type = WEB_LINK, value = null)

        // when
        sut.handle(view, action)

        // then
        verify { uriHandler wasNot Called }
    }

    @Test
    fun `handle uri`() {
        // given
        val activity = mockk<Activity>()
        val view = mockk<InAppMessageLayout> {
            every { this@mockk.activity } returns activity
        }
        val action = InAppMessages.action(type = WEB_LINK, value = "gogo")

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            uriHandler.handle(activity, "gogo")
        }
    }
}
