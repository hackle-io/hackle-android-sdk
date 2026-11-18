package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext

internal class InAppMessagePresentResponse(
    val code: Code,
    val context: InAppMessagePresentationContext,
) {
    enum class Code {
        PRESENT,
        ACTIVITY_NOT_FOUND,
        ALREADY_PRESENTED,
        UNSUPPORTED_ORIENTATION,
        IN_PROGRESS,
        EXCEPTION,
    }

    override fun toString(): String {
        return "InAppMessagePresentResponse(code=${code}, dispatchId=${context.dispatchId}, inAppMessage=${context.inAppMessage}, displayType=${context.message.layout.displayType}, layoutType=${context.message.layout.layoutType})"
    }

    companion object {
        fun of(
            code: Code,
            context: InAppMessagePresentationContext,
        ): InAppMessagePresentResponse {
            return InAppMessagePresentResponse(
                code = code,
                context = context
            )
        }
    }
}
