package io.hackle.android.internal.inappmessage.present

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessagePresentRequestTest {

    @Test
    fun `instance`() {

        val deliverRequest = InAppMessages.deliverRequest(
            dispatchId = "111",
            properties = mapOf("\$trigger_event_insert_id" to "insert_id")
        )
        val inAppMessage = InAppMessages.create()
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "device_id").build()
        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(
            reason = DecisionReason.OVERRIDDEN
        )
        val layoutEvaluation = InAppMessages.layoutEvaluation(
            properties = mapOf(
                "experiment_id" to 42L,
                "experiment_key" to 320L,
                "variation_id" to 1001L,
                "variation_key" to "B",
                "experiment_decision_reason" to "TRAFFIC_ALLOCATED"
            )
        )

        val presentRequest =
            InAppMessagePresentRequest.of(deliverRequest, inAppMessage, user, eligibilityEvaluation, layoutEvaluation)

        expectThat(presentRequest) {
            get { dispatchId } isEqualTo "111"
            get { reason } isEqualTo DecisionReason.OVERRIDDEN
            get { properties } isEqualTo mapOf(
                "\$trigger_event_insert_id" to "insert_id",
                "experiment_id" to 42L,
                "experiment_key" to 320L,
                "variation_id" to 1001L,
                "variation_key" to "B",
                "experiment_decision_reason" to "TRAFFIC_ALLOCATED"
            )
        }
    }
}
