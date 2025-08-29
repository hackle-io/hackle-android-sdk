package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse

internal data class InAppMessageDeliverResponse(
    val dispatchId: String,
    val inAppMessageKey: Long,
    val code: Code,
    val presentResponse: InAppMessagePresentResponse?,
) {

    enum class Code {
        PRESENT,
        ACTIVITY_INACTIVE,
        WORKSPACE_NOT_FOUND,
        IN_APP_MESSAGE_NOT_FOUND,
        IDENTIFIER_CHANGED,
        INELIGIBLE,
        EXCEPTION
    }

    override fun toString(): String {
        return "InAppMessageDeliverResponse(dispatchId=$dispatchId, inAppMessageKey=$inAppMessageKey, code=$code)"
    }

    companion object {
        fun of(
            request: InAppMessageDeliverRequest,
            code: Code,
            presentResponse: InAppMessagePresentResponse? = null,
        ): InAppMessageDeliverResponse {
            return InAppMessageDeliverResponse(
                dispatchId = request.dispatchId,
                inAppMessageKey = request.inAppMessageKey,
                code = code,
                presentResponse = presentResponse
            )
        }
    }
}
