package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.inappmessage.delay.InAppMessageDelay
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse

internal data class InAppMessageScheduleResponse(
    val dispatchId: String,
    val inAppMessageKey: Long,
    val code: Code,
    val deliverResponse: InAppMessageDeliverResponse?,
    val delay: InAppMessageDelay?,
) {
    enum class Code {
        DELIVER,
        DELAY,
        IGNORE,
        EXCEPTION
    }

    override fun toString(): String {
        return "InAppMessageScheduleResponse(dispatchId=$dispatchId, inAppMessageKey=$inAppMessageKey, code=$code)"
    }

    companion object {
        fun of(
            request: InAppMessageScheduleRequest,
            code: Code,
            deliverResponse: InAppMessageDeliverResponse? = null,
            delay: InAppMessageDelay? = null,
        ): InAppMessageScheduleResponse {
            return InAppMessageScheduleResponse(
                dispatchId = request.schedule.dispatchId,
                inAppMessageKey = request.schedule.inAppMessageKey,
                code = code,
                deliverResponse = deliverResponse,
                delay = delay
            )
        }
    }
}
