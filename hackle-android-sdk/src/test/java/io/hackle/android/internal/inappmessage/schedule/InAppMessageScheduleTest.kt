package io.hackle.android.internal.inappmessage.schedule

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.trigger.InAppMessageTrigger
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageScheduleTest {

    @Test
    fun `create`() {

        val inAppMessage = InAppMessages.create(
            key = 42
        )
        val evaluation = InAppMessageEvaluation(true, DecisionReason.OVERRIDDEN)
        val event = UserEvents.track(
            eventKey = "test",
            timestamp = 320,
            insertId = "insert"
        )
        val trigger = InAppMessageTrigger(inAppMessage, evaluation, event)

        val schedule = InAppMessageSchedule.create(trigger)

        expectThat(schedule) {
            get { this.inAppMessageKey } isEqualTo 42L
            get { this.time } isEqualTo InAppMessageSchedule.Time(320, 320)
            get { this.evaluation } isEqualTo evaluation
            get { this.eventBasedContext.insertId } isEqualTo "insert"
            get { this.eventBasedContext.event.key } isEqualTo "test"
        }

        expectThat(schedule.time.delayMillis(300)) isEqualTo 20L
    }
}
