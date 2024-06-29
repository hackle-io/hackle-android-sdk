package io.hackle.android.ui.inappmessage.event

import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.sdk.core.model.InAppMessage.ActionType.CLOSE
import io.hackle.sdk.core.model.InAppMessage.ActionType.WEB_LINK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


internal class InAppMessageCloseActionHandlerTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageCloseActionHandler()
        expectThat(sut.supports(InAppMessages.action(type = CLOSE))).isTrue()
        expectThat(sut.supports(InAppMessages.action(type = WEB_LINK))).isFalse()
    }

    @Test
    fun `handle - close view`() {
        // given
        val sut = InAppMessageCloseActionHandler()

        val view = mockk<InAppMessageLayout>(relaxUnitFun = true)
        val action = InAppMessages.action(type = CLOSE)

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            view.close()
        }
    }
}
