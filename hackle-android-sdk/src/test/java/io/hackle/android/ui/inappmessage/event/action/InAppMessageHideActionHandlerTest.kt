package io.hackle.android.ui.inappmessage.event.action

import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageHiddenStorage
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


internal class InAppMessageHideActionHandlerTest {

    @RelaxedMockK
    private lateinit var storage: AndroidInAppMessageHiddenStorage

    @MockK
    private lateinit var clock: Clock

    @InjectMockKs
    private lateinit var sut: InAppMessageHideActionHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `supports`() {
        expectThat(sut.supports(InAppMessages.action(type = InAppMessage.ActionType.CLOSE))).isFalse()
        expectThat(sut.supports(InAppMessages.action(type = InAppMessage.ActionType.HIDDEN))).isTrue()
    }

    @Test
    fun `handle - default`() {
        // given
        val context = InAppMessages.context()
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { this@mockk.presentationContext } returns context
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.HIDDEN)

        every { clock.currentMillis() } returns 42

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            storage.put(any(), (1000 * 60 * 60 * 24) + 42)
        }
        verify(exactly = 1) {
            view.close()
        }
    }


    @Test
    fun `handle - custom`() {
        // given
        val context = InAppMessages.context()
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { this@mockk.presentationContext } returns context
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.HIDDEN, value = "100")

        every { clock.currentMillis() } returns 42

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 1) {
            storage.put(any(), 142)
        }
        verify(exactly = 1) {
            view.close()
        }
    }

    @Test
    fun `handle - override`() {
        // given
        val context = InAppMessages.context(decisionReason = DecisionReason.OVERRIDDEN)
        val view = mockk<InAppMessageView>(relaxUnitFun = true) {
            every { this@mockk.presentationContext } returns context
        }
        val action = InAppMessages.action(type = InAppMessage.ActionType.HIDDEN)

        every { clock.currentMillis() } returns 42

        // when
        sut.handle(view, action)

        // then
        verify(exactly = 0) {
            storage.put(any(), any())
        }
        verify(exactly = 1) {
            view.close()
        }
    }
}
