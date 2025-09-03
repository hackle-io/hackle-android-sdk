package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.inappmessage.InAppMessageManager
import io.hackle.android.internal.inappmessage.reset.InAppMessageResetProcessor
import io.hackle.sdk.common.User
import io.hackle.sdk.core.event.UserEvent
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class InAppMessageManagerTest {

    @RelaxedMockK
    private lateinit var triggerProcessor: InAppMessageTriggerProcessor

    @RelaxedMockK
    private lateinit var resetProcessor: InAppMessageResetProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageManager

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when onEvent then trigger in-app messages`() {
        // given
        val event = mockk<UserEvent>()

        // when
        sut.onEvent(event)

        // then
        verify(exactly = 1) {
            triggerProcessor.process(event)
        }
    }

    @Test
    fun `when onUserUpdated then reset in-app messages`() {
        // given
        val oldUser = mockk<User>()
        val newUser = mockk<User>()

        // when
        sut.onUserUpdated(oldUser, newUser, 42)

        // then
        verify(exactly = 1) {
            resetProcessor.process(oldUser, newUser)
        }
    }
}
