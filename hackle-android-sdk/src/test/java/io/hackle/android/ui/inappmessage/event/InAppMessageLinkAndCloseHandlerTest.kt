package io.hackle.android.ui.inappmessage.event

import android.app.Activity
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.close
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageLinkAndCloseHandlerTest {

    @RelaxedMockK
    private lateinit var uriHandler: UriHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageLinkAndCloseActionHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessages.action(type = InAppMessage.ActionType.LINK_AND_CLOSE))).isTrue()
        expectThat(sut.supports(InAppMessages.action(type = InAppMessage.ActionType.WEB_LINK))).isFalse()
    }

    @Test
    fun `handle - when activity is null then do nothing`() {
        // given
        val view = mockk<InAppMessageView> {
            every { activity } returns null
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.LINK_AND_CLOSE)

        // when
        sut.handle(view, action)

        // then
        verify { uriHandler wasNot Called }
    }

    @Test
    fun `when action value is null then do nothing`() {
        // given
        val view = mockk<InAppMessageView> {
            every { activity } returns mockk()
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.LINK_AND_CLOSE, value = null)

        // when
        sut.handle(view, action)

        // then
        verify { uriHandler wasNot Called }
    }

    @Test
    fun `handle uri and close`() {
        // given
        val activity = mockk<Activity>()
        val view = mockk<InAppMessageView>(relaxUnitFun = true) {
            every { this@mockk.activity } returns activity
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.LINK_AND_CLOSE, value = "gogo")

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            view.close()
        }
        verify(exactly = 1) {
            uriHandler.handle(activity, "gogo")
        }
    }
}
