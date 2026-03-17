package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.support.assertThrows
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isTrue

internal class CloseInAppMessageViewInvocationHandlerTest : AbstractInvocationHandlerTest() {

    @MockK
    private lateinit var core: HackleAppCore

    @InjectMockKs
    private lateinit var sut: CloseInAppMessageViewInvocationHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(TaskExecutors)
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
    }

    @Test
    fun `when parameters viewId is null then throws exception`() {
        val request = request(InvocationCommand.CLOSE_IN_APP_MESSAGE_VIEW, mapOf("viewId" to null))
        assertThrows<IllegalStateException> { sut.invoke(request) }
    }

    @Test
    fun `when not found view for viewId then do nothing`() {
        // given
        every { core.getInAppMessageView(any()) } returns null
        val request = request(InvocationCommand.CLOSE_IN_APP_MESSAGE_VIEW, mapOf("viewId" to "view-id"))

        // when
        val actual = sut.invoke(request)

        // then
        expectThat(actual) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 0) {
            TaskExecutors.runOnUiThread(any())
        }
    }

    @Test
    fun `when view is exists then close that view`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true)
        every { core.getInAppMessageView(any()) } returns view

        val request = request(InvocationCommand.CLOSE_IN_APP_MESSAGE_VIEW, mapOf("viewId" to "view-id"))

        // when
        val actual = sut.invoke(request)

        // then
        expectThat(actual) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) {
            TaskExecutors.runOnUiThread(any())
        }
        verify(exactly = 1) {
            view.close()
        }
    }
}
