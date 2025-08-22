package io.hackle.android.internal.inappmessage.reset

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.trigger.InAppMessageIdentifierChecker
import io.hackle.sdk.common.User
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class InAppMessageResetProcessorTest {
    @MockK
    private lateinit var identifierChecker: InAppMessageIdentifierChecker

    @RelaxedMockK
    private lateinit var delayManager: InAppMessageDelayManager

    @InjectMockKs
    private lateinit var sut: InAppMessageResetProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when identifier changed then reset cancel all delays`() {
        // given
        val oldUser = User.builder().deviceId("a").build()
        val newUser = User.builder().deviceId("b").build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns true

        // when
        sut.process(oldUser, newUser)

        // then
        verify(exactly = 1) {
            delayManager.cancelAll()
        }
    }

    @Test
    fun `when identifier not changed then do nothing`() {
        // given
        val oldUser = User.builder().deviceId("a").build()
        val newUser = User.builder().deviceId("a").build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false


        // when
        sut.process(oldUser, newUser)

        // then
        verify(exactly = 0) {
            delayManager.cancelAll()
        }
    }

    @Test
    fun `exception`() {
        val oldUser = User.builder().deviceId("a").build()
        val newUser = User.builder().deviceId("b").build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns true
        every { delayManager.cancelAll() } throws IllegalArgumentException("fail")

        sut.process(oldUser, newUser)
    }
}
