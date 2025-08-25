package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext

internal class InAppMessagePresentResponse(
    val dispatchId: String,
    val context: InAppMessagePresentationContext,
) {

    override fun toString(): String {
        return "InAppMessagePresentResponse(dispatchId=$dispatchId, inAppMessage=${context.inAppMessage}, displayType=${context.message.layout.displayType}, layoutType=${context.message.layout.layoutType})"
    }

    companion object {
        fun of(
            request: InAppMessagePresentRequest,
            context: InAppMessagePresentationContext,
        ): InAppMessagePresentResponse {
            return InAppMessagePresentResponse(
                dispatchId = request.dispatchId,
                context = context
            )
        }
    }
}
