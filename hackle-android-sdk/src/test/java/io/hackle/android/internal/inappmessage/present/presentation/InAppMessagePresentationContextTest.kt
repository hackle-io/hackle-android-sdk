package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class InAppMessagePresentationContextTest {

    @Test
    fun `of - present request의 정보로 presentation context를 생성한다`() {
        // given
        val request = InAppMessages.presentRequest(
            dispatchId = "111",
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            properties = mapOf("experiment_id" to 42L),
            triggerEvent = Event.of("trigger_key"),
        )

        // when
        val actual = InAppMessagePresentationContext.of(request)

        // then
        expectThat(actual) {
            get { dispatchId } isEqualTo "111"
            get { inAppMessage } isSameInstanceAs request.inAppMessage
            get { message } isSameInstanceAs request.message
            get { user } isSameInstanceAs request.user
            get { decisionReason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
            get { properties } isEqualTo mapOf("experiment_id" to 42L)
            get { triggerEvent } isSameInstanceAs request.triggerEvent
        }
    }
}
