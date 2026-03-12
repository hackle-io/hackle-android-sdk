package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.internal.invocator.model.HandleInAppMessageViewInvocationDto
import io.hackle.android.internal.invocator.model.InAppMessageElementDto
import io.hackle.android.internal.invocator.model.InAppMessageViewEventDto
import io.hackle.android.internal.utils.json.gsonTypeRef
import io.hackle.android.internal.workspace.InAppMessageDto
import io.hackle.android.support.assertThrows
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleProcessor
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.ACTION
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType.TRACK
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isTrue

internal class HandleInAppMessageViewInvocationHandlerTest : AbstractInvocationHandlerTest() {

    @MockK
    private lateinit var core: HackleAppCore

    @InjectMockKs
    private lateinit var sut: HandleInAppMessageViewInvocationHandler

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when not found view for viewId then do nothing`() {
        // given
        every { core.getInAppMessageView(any()) } returns null

        val request = request(InvocationCommand.HANDLE_IN_APP_MESSAGE_VIEW, parameters(viewId = "view-id"))

        // when
        val actual = sut.invoke(request)

        // then
        expectThat(actual) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
    }

    @Test
    fun `when view is exists then handle event`() {
        // given
        val view = mockk<InAppMessageView>(relaxed = true)

        val eventHandleProcessor = mockk<InAppMessageViewEventHandleProcessor>(relaxed = true)
        every { view.controller.ui.eventHandleProcessor } returns eventHandleProcessor
        every { core.getInAppMessageView(any()) } returns view

        val request = request(InvocationCommand.HANDLE_IN_APP_MESSAGE_VIEW, parameters(viewId = "view-id"))

        // when
        val actual = sut.invoke(request)

        // then
        expectThat(actual) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) {
            eventHandleProcessor.process(any(), any(), listOf(TRACK, ACTION))
        }
    }

    @Test
    fun `unsupported type`() {
        val view = mockk<InAppMessageView>(relaxed = true)

        val eventHandleProcessor = mockk<InAppMessageViewEventHandleProcessor>(relaxed = true)
        every { view.controller.ui.eventHandleProcessor } returns eventHandleProcessor
        every { core.getInAppMessageView(any()) } returns view


        for (type in InAppMessageViewEvent.Type.values().filter { it != InAppMessageViewEvent.Type.ACTION }) {

            val request =
                request(InvocationCommand.HANDLE_IN_APP_MESSAGE_VIEW, parameters(viewId = "view-id", type = type.name))

            assertThrows<IllegalArgumentException> {
                sut.invoke(request)
            }
        }
    }

    private fun parameters(
        viewId: String,
        handleTypes: List<String> = listOf("TRACK", "ACTION"),
        type: String = "ACTION",
        action: InAppMessageDto.MessageContextDto.ActionDto? = InAppMessageDto.MessageContextDto.ActionDto(
            type = "LINK_AND_CLOSE",
            behavior = "CLICK",
            value = "https://hackle.io"
        ),
        element: InAppMessageElementDto? = InAppMessageElementDto(
            elementId = "element-id",
            area = null
        )
    ): Map<String, Any?> {
        val dto = HandleInAppMessageViewInvocationDto(
            viewId = viewId,
            handleTypes = handleTypes,
            event = InAppMessageViewEventDto(
                type = type,
                action = action,
                element = element
            )
        )
        return gson.fromJson(gson.toJson(dto), gsonTypeRef<Map<String, Any?>>().type)
    }
}
