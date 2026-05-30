package io.hackle.android.internal.inappmessage.present.record

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.target.InAppMessageImpression
import io.hackle.sdk.core.evaluation.target.InAppMessageImpressionStorage
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageRecorder(
    private val storage: InAppMessageImpressionStorage,
) {

    // record는 trigger 이벤트 스레드와 delay 발화 스레드에서 동시에 호출될 수 있다.
    // get-modify-set이 비원자적이면 동일 메시지 impression이 유실되므로 직렬화한다.
    private val lock = Any()

    fun record(request: InAppMessagePresentRequest, response: InAppMessagePresentResponse) {
        if (request.reason == DecisionReason.OVERRIDDEN) {
            return
        }

        try {
            synchronized(lock) {
                doRecord(request)
            }
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