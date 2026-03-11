package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.invocation.parameters
import io.hackle.android.internal.invocator.model.HackleInAppMessageViewDto
import io.hackle.android.internal.invocator.model.InAppMessageViewEventDto
import io.hackle.android.internal.invocator.model.HandleInAppMessageViewInvocationDto
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.android.internal.invocator.viewId
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.internal.workspace.toActionOrNull
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewProvider
import io.hackle.android.ui.inappmessage.view.handle
import io.hackle.sdk.core.model.InAppMessage

internal class GetCurrentInAppMessageViewInvocationHandler(
    private val viewProvider: InAppMessageViewProvider
) : InvocationHandler<HackleInAppMessageViewDto?> {
    override fun invoke(request: InvocationRequest): InvocationResponse<HackleInAppMessageViewDto?> {
        val view = viewProvider.currentView?.toDto()
        return InvocationResponse.success(view)
    }
}

internal class CloseInAppMessageViewInvocationHandler(
    private val viewProvider: InAppMessageViewProvider,
) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val viewId = requireNotNull(request.parameters.viewId())
        val view = viewProvider.get(viewId) ?: return InvocationResponse.success()
        runOnUiThread {
            view.close()
        }
        return InvocationResponse.success()
    }
}

internal class HandleInAppMessageViewInvocationHandler(
    private val viewProvider: InAppMessageViewProvider
) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val dto = request.parameters<HandleInAppMessageViewInvocationDto>()
        val view = viewProvider.get(dto.viewId) ?: return InvocationResponse.success()
        val event = inAppMessageEvent(view, dto.event)
        val handleTypes = dto.handleTypes.map { InAppMessageViewEventHandleType.valueOf(it) }
        view.handle(event, handleTypes)
        return InvocationResponse.success()
    }

    private fun inAppMessageEvent(view: InAppMessageView, dto: InAppMessageViewEventDto): InAppMessageViewEvent {
        return when (InAppMessageViewEvent.Type.valueOf(dto.type)) {
            InAppMessageViewEvent.Type.ACTION -> actionEvent(view, dto)
            InAppMessageViewEvent.Type.IMPRESSION,
            InAppMessageViewEvent.Type.CLOSE,
            InAppMessageViewEvent.Type.IMAGE_IMPRESSION -> throw IllegalArgumentException("Unsupported InAppMessageEvent.Type [${dto.type}]")
        }
    }

    private fun actionEvent(view: InAppMessageView, dto: InAppMessageViewEventDto): InAppMessageViewEvent.Action {
        val action = requireNotNull(dto.action)
        val element = requireNotNull(dto.element)
        return InAppMessageViewEvent.action(
            view = view,
            action = requireNotNull(action.toActionOrNull()),
            area = element.area?.let { InAppMessage.ActionArea.valueOf(it) },
            elementId = element.elementId
        )
    }
}
