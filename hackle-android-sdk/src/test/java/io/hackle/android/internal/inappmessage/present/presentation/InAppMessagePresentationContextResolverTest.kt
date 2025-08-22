package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluation
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluator
import io.hackle.sdk.core.user.HackleUser
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessagePresentationContextResolverTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var layoutEvaluator: InAppMessageLayoutEvaluator

    @InjectMockKs
    private lateinit var sut: InAppMessagePresentationContextResolver

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `resolve`() {
        // given
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

        val message = inAppMessage.messageContext.messages.first()
        val evaluation = InAppMessageLayoutEvaluation(
            reason = DecisionReason.IN_APP_MESSAGE_TARGET,
            targetEvaluations = emptyList(),
            inAppMessage = inAppMessage,
            message = message,
            properties = mapOf("layout" to "evaluation")
        )

        every { core.evaluate(any(), any(), layoutEvaluator) } returns evaluation

        // when
        val actual = sut.resolve(request)

        // then
        expectThat(actual) {
            get { this.dispatchId } isEqualTo "111"
            get { this.message } isEqualTo message
            get { this.properties } isEqualTo mapOf(
                "present" to "request",
                "layout" to "evaluation"
            )
        }
    }
}
