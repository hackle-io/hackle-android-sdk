package io.hackle.android.ui.inappmessage.event.action

import android.app.Activity
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionType.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


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
        for (actionType in InAppMessage.ActionType.values()) {
            expectThat(sut.supports(InAppMessages.action(type = actionType))).isEqualTo(
                actionType in listOf(
                    WEB_LINK,
                    LINK_NEW_TAB,
                    LINK_NEW_WINDOW
                )
            )
        }
    }

    @Test
    fun `handle - when activity is null then do nothing`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { activity } returns null
            every { presentationContext } returns mockk(relaxed = true)
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
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { activity } returns mockk()
            every { presentationContext } returns mockk(relaxed = true)
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
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { this@mockk.activity } returns activity
            every { presentationContext } returns mockk(relaxed = true)
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
