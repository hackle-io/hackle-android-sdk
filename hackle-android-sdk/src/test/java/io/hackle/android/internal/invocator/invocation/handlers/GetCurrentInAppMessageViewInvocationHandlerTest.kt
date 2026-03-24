package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

internal class GetCurrentInAppMessageViewInvocationHandlerTest : AbstractInvocationHandlerTest() {

    @MockK
    private lateinit var core: HackleAppCore

    @InjectMockKs
    private lateinit var sut: GetCurrentInAppMessageViewInvocationHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when current view does not exists then returns null`() {
        // given
        every { core.currentInAppMessageView } returns null

        val request = request(InvocationCommand.GET_CURRENT_IN_APP_MESSAGE_VIEW)

        // when
        val actual = sut.invoke(request)

        // then
        expectThat(actual) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
    }

    @Test
    fun `when current view exists then returns that view`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true) {
            every { id } returns "42"
        }
        every { core.currentInAppMessageView } returns view

        val request = request(InvocationCommand.GET_CURRENT_IN_APP_MESSAGE_VIEW)

        // when
        val action = sut.invoke(request)

        // then
        expectThat(action) {
            get { isSuccess }.isTrue()
            get { data }.isNotNull().and {
                get { id } isEqualTo "42"
            }
        }
    }
}
