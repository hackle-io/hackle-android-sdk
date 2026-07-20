package io.hackle.android.internal.inappmessage.present.record

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse.Code
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpression
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpressionStorage
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageRecorder(
    private val storage: InAppMessageImpressionStorage,
) {

    fun record(request: InAppMessagePresentRequest, response: InAppMessagePresentResponse) {
        if (response.code != Code.PRESENT) {
            return
        }

        if (request.reason == DecisionReason.OVERRIDDEN) {
            return
        }

        try {
            doRecord(request)
        } catch (e: Exception) {
            log.error { "Failed to record InAppMessageImpression: $e" }
        }
    }

    private fun doRecord(request: InAppMessagePresentRequest) {
        val impressions = storage.get(request.inAppMessage)
        val impression = InAppMessageImpression(request.user.identifiers, request.requestedAt)

        val newImpressions = impressions + impression

        val impressionToSave = if (newImpressions.size > STORE_MAX_SIZE) {
            newImpressions.drop(newImpressions.size - STORE_MAX_SIZE)
        } else {
            newImpressions
        }
        storage.set(request.inAppMessage, impressionToSave)
        log.debug { "InAppMessage Impression recorded: dispatchId=${request.dispatchId}, inAppMessageKey=${request.inAppMessage.key}, impression=${impression}" }
    }

    companion object {
        private val log = Logger<InAppMessageRecorder>()
        private const val STORE_MAX_SIZE = 100
    }
}
