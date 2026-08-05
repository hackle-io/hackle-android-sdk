package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.Identifiers
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class InAppMessageDeliverRequestTest {

    @Test
    fun `instance`() {
        val event = Event.of("test")
        val scheduleIdentifiers = Identifiers.from(User.builder().deviceId("device_id").build())
        val schedule = InAppMessages.schedule(
            dispatchId = "111",
            inAppMessageKey = 320,
            identifiers = scheduleIdentifiers,
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            eventBasedContext = InAppMessageSchedule.EventBasedContext(insertId = "222", event = event)
        )
        val scheduleRequest = schedule.toRequest(InAppMessageScheduleType.TRIGGERED, 42)

        val deliverRequest = InAppMessageDeliverRequest.of(scheduleRequest)
        expectThat(deliverRequest) {
            get { dispatchId } isEqualTo "111"
            get { inAppMessageKey } isEqualTo 320L
            get { identifiers } isSameInstanceAs scheduleIdentifiers
            get { requestedAt } isEqualTo 42L
            get { reason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
            get { properties } isEqualTo mapOf("\$trigger_event_insert_id" to "222")
            get { triggerEvent } isSameInstanceAs event
        }
    }
}
