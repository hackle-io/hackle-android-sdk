package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.Event
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageDeliverRequestTest {

    @Test
    fun `instance`() {
        val schedule = InAppMessages.schedule(
            dispatchId = "111",
            eventBasedContext = InAppMessageSchedule.EventBasedContext(insertId = "222", event = Event.of("test"))
        )
        val scheduleRequest = schedule.toRequest(InAppMessageScheduleType.TRIGGERED, 42)


        val deliverRequest = InAppMessageDeliverRequest.of(scheduleRequest)
        expectThat(deliverRequest) {
            get { dispatchId } isEqualTo "111"
            get { requestedAt } isEqualTo 42L
            get { properties } isEqualTo mapOf("\$trigger_event_insert_id" to "222")
        }
    }
}
