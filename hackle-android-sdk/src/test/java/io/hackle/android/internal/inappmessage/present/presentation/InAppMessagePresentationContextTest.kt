package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class InAppMessagePresentationContextTest {

    @Test
    fun `of - propagates triggerEvent from PresentRequest`() {
        val event = Event.of("trigger_key")
        val deliverRequest = InAppMessages.deliverRequest(triggerEvent = event)
        val inAppMessage = InAppMessages.create()
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "device_id").build()
        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(reason = DecisionReason.IN_APP_MESSAGE_TARGET)
        val layoutEvaluation = InAppMessages.layoutEvaluation()
        val presentRequest = InAppMessagePresentRequest.of(
            deliverRequest, inAppMessage, user, eligibilityEvaluation, layoutEvaluation
        )

        val context = InAppMessagePresentationContext.of(presentRequest)

        expectThat(context) {
            get { dispatchId } isEqualTo presentRequest.dispatchId
            get { triggerEvent } isSameInstanceAs event
        }
    }
}
