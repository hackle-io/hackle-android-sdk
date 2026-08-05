package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluation
import io.hackle.android.support.Experiments
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class InAppMessagePresentRequestTest {

    @Test
    fun `of - deliver request와 deliver evaluation으로 present request를 생성한다`() {
        // given
        val event = Event.of("trigger_key")
        val deliverRequest = InAppMessages.deliverRequest(
            dispatchId = "111",
            requestedAt = 42,
            properties = mapOf("\$trigger_event_insert_id" to "insert_id"),
            triggerEvent = event,
        )
        val requestUser = HackleUser.builder().identifier(IdentifierType.DEVICE, "device_id").build()

        val entity = InAppMessages.config(key = 1)
        val layoutMessage = InAppMessages.message()
        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(
                evaluation = InAppMessages.eligibilityEvaluation(
                    inAppMessage = entity,
                    result = InAppMessages.eligibilityResult(
                        isEligible = true,
                        reason = DecisionReason.IN_APP_MESSAGE_TARGET
                    )
                )
            ),
            layout = InAppMessages.layoutResponse(
                evaluation = InAppMessages.layoutEvaluation(
                    result = InAppMessages.layoutResult(message = layoutMessage)
                ),
                experiment = Experiments.evaluation(
                    entity = Experiments.config(id = 42, key = 320),
                    result = Experiments.result(
                        reason = DecisionReason.TRAFFIC_ALLOCATED,
                        variation = Experiments.variation(id = 1001, key = "B")
                    )
                )
            ),
        )

        // when
        val actual = InAppMessagePresentRequest.of(deliverRequest, requestUser, evaluation)

        // then
        expectThat(actual) {
            get { dispatchId } isEqualTo "111"
            get { inAppMessage } isSameInstanceAs entity
            get { message } isSameInstanceAs layoutMessage
            get { user } isSameInstanceAs requestUser
            get { requestedAt } isEqualTo 42L
            get { reason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
            get { properties } isEqualTo mapOf(
                "\$trigger_event_insert_id" to "insert_id",
                "experiment_id" to 42L,
                "experiment_key" to 320L,
                "variation_id" to 1001L,
                "variation_key" to "B",
                "experiment_decision_reason" to "TRAFFIC_ALLOCATED",
            )
            get { triggerEvent } isSameInstanceAs event
        }
    }
}
