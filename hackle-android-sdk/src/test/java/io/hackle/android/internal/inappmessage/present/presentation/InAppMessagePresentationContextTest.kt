package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluation
import io.hackle.sdk.core.user.HackleUser
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessagePresentationContextTest {

    @Test
    fun `instance`() {
        val inAppMessage = InAppMessages.create()
        val request = InAppMessagePresentRequest(
            dispatchId = "111",
            workspace = mockk(),
            inAppMessage = inAppMessage,
            user = HackleUser.builder().build(),
            requestedAt = 42,
            evaluation = InAppMessageEvaluation(true, DecisionReason.IN_APP_MESSAGE_TARGET),
            properties = mapOf("present" to "request")
        )

        val evaluation = InAppMessageLayoutEvaluation(
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            targetEvaluations = emptyList(),
            inAppMessage = inAppMessage,
            message = inAppMessage.messageContext.messages.first(),
            properties = mapOf("layout" to "evaluation")
        )

        val context = InAppMessagePresentationContext.of(request, evaluation)

        expectThat(context) {
            get { dispatchId } isEqualTo "111"
            get { properties } isEqualTo mapOf(
                "present" to "request",
                "layout" to "evaluation"
            )
        }
    }
}
