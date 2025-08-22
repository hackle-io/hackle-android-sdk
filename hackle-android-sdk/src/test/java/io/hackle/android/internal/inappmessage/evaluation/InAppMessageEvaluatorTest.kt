package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluator
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageEvaluatorTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var eligibilityEvaluator: InAppMessageEligibilityEvaluator

    @InjectMockKs
    private lateinit var sut: InAppMessageEvaluator

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `evaluate`() {
        // given
        val workspace = mockk<Workspace>()
        val inAppMessage = InAppMessages.create()
        val user = HackleUser.builder().identifier(IdentifierType.DEVICE, "iam").build()

        val evaluation =
            InAppMessageEligibilityEvaluation(DecisionReason.OVERRIDDEN, emptyList(), inAppMessage, true)
        every { core.evaluate(any(), any(), eligibilityEvaluator) } returns evaluation

        // when
        val actual = sut.evaluate(workspace, inAppMessage, user, 42)

        // then
        expectThat(actual) isEqualTo InAppMessageEvaluation(true, DecisionReason.OVERRIDDEN)
    }
}
