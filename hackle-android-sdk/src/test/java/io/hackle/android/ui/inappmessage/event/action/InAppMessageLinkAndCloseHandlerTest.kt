package io.hackle.android.ui.inappmessage.event.action

import android.app.Activity
import android.net.Uri
import io.hackle.android.HackleUrlChannel
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.core.HackleUrlDispatcher
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionType.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class InAppMessageLinkAndCloseHandlerTest {

    @RelaxedMockK
    private lateinit var urlDispatcher: HackleUrlDispatcher

    @InjectMockKs
    private lateinit var sut: InAppMessageLinkAndCloseActionHandler

    private val uri = mockk<Uri>()

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(Uri::class)
        every { Uri.parse("gogo") } returns uri
    }

    @After
    fun after() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `supports`() {
        for (actionType in InAppMessage.ActionType.values()) {
            expectThat(sut.supports(InAppMessages.action(type = actionType))).isEqualTo(
                actionType in listOf(
                    LINK_AND_CLOSE,
                    LINK_NEW_TAB_AND_CLOSE,
                    LINK_NEW_WINDOW_AND_CLOSE
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
        val action = InAppMessages.action(type = LINK_AND_CLOSE)

        // when
        sut.handle(view, action)

        // then
        verify { urlDispatcher wasNot Called }
    }

    @Test
    fun `when action value is null then do nothing`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { activity } returns mockk()
            every { presentationContext } returns mockk(relaxed = true)
        }
        val action = InAppMessages.action(type = LINK_AND_CLOSE, value = null)

        // when
        sut.handle(view, action)

        // then
        verify { urlDispatcher wasNot Called }
    }

    @Test
    fun `handle uri and close`() {
        // given
        val activity = mockk<Activity>()
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { this@mockk.activity } returns activity
            every { presentationContext } returns mockk(relaxed = true)
        }
        val action = InAppMessages.action(type = LINK_AND_CLOSE, value = "gogo")

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            view.close()
        }
        verify(exactly = 1) {
            urlDispatcher.open(activity, uri, HackleUrlChannel.IN_APP_MESSAGE, null)
        }
    }
}
