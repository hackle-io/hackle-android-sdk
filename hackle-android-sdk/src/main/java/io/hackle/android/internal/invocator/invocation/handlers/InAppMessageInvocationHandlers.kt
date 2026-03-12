package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.invocation.parameters
import io.hackle.android.internal.invocator.model.HackleInAppMessageViewDto
import io.hackle.android.internal.invocator.model.HandleInAppMessageViewInvocationDto
import io.hackle.android.internal.invocator.model.InAppMessageViewEventDto
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.android.internal.invocator.viewId
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.internal.workspace.toActionOrNull
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.handle
import io.hackle.sdk.core.model.InAppMessage

internal class GetCurrentInAppMessageViewInvocationHandler(
    private val core: HackleAppCore
) : InvocationHandler<HackleInAppMessageViewDto> {
    override fun invoke(request: InvocationRequest): InvocationResponse<HackleInAppMessageViewDto> {
        val view = core.currentInAppMessageView ?: return InvocationResponse.success()
        val viewDto = view.toDto()
        return InvocationResponse.success(viewDto)
    }
}

internal class CloseInAppMessageViewInvocationHandler(
    private val core: HackleAppCore
) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val viewId = requireNotNull(request.parameters.viewId()) { "parameters.viewId must not be null." }
        val view = core.getInAppMessageView(viewId) ?: return InvocationResponse.success()
        runOnUiThread {
            view.close()
        }
        return InvocationResponse.success()
    }
}

internal class HandleInAppMessageViewInvocationHandler(
    private val core: HackleAppCore
) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val dto = request.parameters<HandleInAppMessageViewInvocationDto>()
        val view = core.getInAppMessageView(dto.viewId) ?: return InvocationResponse.success()
        val event = viewEvent(view, dto.event)
        val handleTypes = dto.handleTypes.map { InAppMessageViewEventHandleType.valueOf(it) }
        view.handle(event, handleTypes)
        return InvocationResponse.success()
    }

    private fun viewEvent(view: InAppMessageView, dto: InAppMessageViewEventDto): InAppMessageViewEvent {
        return when (InAppMessageViewEvent.Type.valueOf(dto.type)) {
            InAppMessageViewEvent.Type.ACTION -> actionEvent(view, dto)
            InAppMessageViewEvent.Type.IMPRESSION,
            InAppMessageViewEvent.Type.CLOSE,
            InAppMessageViewEvent.Type.IMAGE_IMPRESSION -> throw IllegalArgumentException("Unsupported InAppMessageViewEvent.Type [${dto.type}]")
        }
    }

    private fun actionEvent(view: InAppMessageView, dto: InAppMessageViewEventDto): InAppMessageViewEvent.Action {
        val action = requireNotNull(dto.action) { "action must not be null" }
        val element = requireNotNull(dto.element) { "element must not be null" }
        return InAppMessageViewEvent.action(
            view = view,
            action = requireNotNull(action.toActionOrNull()) { "Invalid action format" },
            area = element.area?.let { InAppMessage.ActionArea.valueOf(it) },
            elementId = element.elementId
        )
    }
}
