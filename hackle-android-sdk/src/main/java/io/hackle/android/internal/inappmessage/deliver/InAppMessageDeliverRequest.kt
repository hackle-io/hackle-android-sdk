package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.model.Identifiers

internal data class InAppMessageDeliverRequest(
    val dispatchId: String,
    val inAppMessageKey: Long,
    val identifiers: Identifiers,
    val requestedAt: Long,
    val evaluation: InAppMessageEvaluation,
    val properties: Map<String, Any>,
) {

    companion object {
        fun of(request: InAppMessageScheduleRequest): InAppMessageDeliverRequest {
            return InAppMessageDeliverRequest(
                dispatchId = request.schedule.dispatchId,
                inAppMessageKey = request.schedule.inAppMessageKey,
                identifiers = request.schedule.identifiers,
                requestedAt = request.requestedAt,
                evaluation = request.schedule.evaluation,
                properties = PropertiesBuilder()
                    .add("trigger_event_insert_id", request.schedule.eventBasedContext.insertId)
                    .build()
            )
        }
    }
}
