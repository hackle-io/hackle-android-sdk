package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.inappmessage.trigger.InAppMessageTrigger
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.model.InAppMessage
import java.util.UUID

internal data class InAppMessageSchedule(
    val dispatchId: String,
    val inAppMessageKey: Long,
    val identifiers: Identifiers,
    val time: Time,
    val reason: DecisionReason,
    val eventBasedContext: EventBasedContext,
) {

    data class Time(val startedAt: Long, val deliverAt: Long) {

        fun delayMillis(at: Long): Long {
            return deliverAt - at
        }

        companion object {
            fun of(inAppMessage: InAppMessage, startedAt: Long): Time {
                return Time(
                    startedAt = startedAt,
                    deliverAt = inAppMessage.eventTrigger.delay.deliverAt(startedAt)
                )
            }
        }
    }

    data class EventBasedContext(
        val insertId: String,
        val event: Event,
    )

    fun toRequest(type: InAppMessageScheduleType, requestedAt: Long): InAppMessageScheduleRequest {
        return InAppMessageScheduleRequest(this, type, requestedAt)
    }

    companion object {
        fun create(trigger: InAppMessageTrigger): InAppMessageSchedule {
            return InAppMessageSchedule(
                dispatchId = UUID.randomUUID().toString(),
                inAppMessageKey = trigger.inAppMessage.key,
                identifiers = Identifiers.from(trigger.event.user.identifiers),
                time = Time.of(
                    inAppMessage = trigger.inAppMessage,
                    startedAt = trigger.event.timestamp
                ),
                reason = trigger.reason,
                eventBasedContext = EventBasedContext(
                    insertId = trigger.event.insertId,
                    event = trigger.event.event
                )
            )
        }
    }
}
