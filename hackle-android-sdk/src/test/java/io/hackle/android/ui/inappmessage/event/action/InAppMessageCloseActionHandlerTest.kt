package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.model.InAppMessage.ActionType.CLOSE
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


internal class InAppMessageCloseActionHandlerTest {

    @Test
    fun `supports`() {
        val sut = InAppMessageCloseActionHandler()
        for (actionType in InAppMessage.ActionType.values()) {
            expectThat(sut.supports(InAppMessages.action(type = actionType))).isEqualTo(actionType == CLOSE)
        }
    }

    @Test
    fun `handle - close view`() {
        // given
        val sut = InAppMessageCloseActionHandler()

        val view = mockk<InAppMessageView>(relaxUnitFun = true)
        val action = InAppMessages.action(type = CLOSE)

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            view.close()
        }
    }
}
