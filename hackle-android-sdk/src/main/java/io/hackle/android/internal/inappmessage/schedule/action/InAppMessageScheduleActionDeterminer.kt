package io.hackle.android.internal.inappmessage.schedule.action

import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleRequest
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageScheduleActionDeterminer {
    fun determine(request: InAppMessageScheduleRequest): InAppMessageScheduleAction {
        val delayMillis = request.delayMillis
        val action = when {
            delayMillis > 0 -> InAppMessageScheduleAction.DELAY
            delayMillis in DELIVER_DURATION_RANGE -> InAppMessageScheduleAction.DELIVER
            delayMillis < DELIVER_DURATION_THRESHOLD_MILLIS -> InAppMessageScheduleAction.IGNORE
            else -> throw IllegalArgumentException("InAppMessageScheduleAction cannot be determined (key=${request.schedule.inAppMessageKey})")
        }
        log.debug { "InAppMessage ScheduleAction determined: action=$action, dispatchId=${request.schedule.dispatchId}" }
        return action
    }

    companion object {

        private val log = Logger<InAppMessageScheduleActionDeterminer>()

        /**
         * Deliver only requests made within the past 1 minute
         */
        private const val DELIVER_DURATION_THRESHOLD_MILLIS = -60 * 1000L

        private val DELIVER_DURATION_RANGE = DELIVER_DURATION_THRESHOLD_MILLIS..0
    }
}