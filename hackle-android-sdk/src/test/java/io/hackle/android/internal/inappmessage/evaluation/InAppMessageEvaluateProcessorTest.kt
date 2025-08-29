package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.EvaluationEventRecorder
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityFlow
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityFlowFactory
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

class InAppMessageEvaluateProcessorTest {

    @MockK
    private lateinit var core: HackleCore

    @MockK
    private lateinit var flowFactory: InAppMessageEligibilityFlowFactory

    @RelaxedMockK
    private lateinit var eventRecorder: EvaluationEventRecorder

    @InjectMockKs
    private lateinit var sut: InAppMessageEvaluateProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `trigger evaluate`() {
        // given
        val request = InAppMessages.eligibilityRequest()
        val flow = mockk<InAppMessageEligibilityFlow>()
        every { flowFactory.triggerFlow() } returns flow

        val evaluation = InAppMessages.eligibilityEvaluation()
        every { core.inAppMessage(request, any(), any<InAppMessageEligibilityEvaluator>()) } returns evaluation

        // when
        val actual = sut.process(InAppMessageEvaluateType.TRIGGER, request)

        // then
        expectThat(actual) isSameInstanceAs evaluation
        verify {
            flowFactory.triggerFlow()
        }
    }

    @Test
    fun `deliver evaluate`() {
        // given
        val request = InAppMessages.eligibilityRequest()
        val flow = mockk<InAppMessageEligibilityFlow>()
        every { flowFactory.deliverFlow(any()) } returns flow

        val evaluation = InAppMessages.eligibilityEvaluation()
        every { core.inAppMessage(request, any(), any<InAppMessageEligibilityEvaluator>()) } returns evaluation

        // when
        val actual = sut.process(InAppMessageEvaluateType.DELIVER, request)

        // then
        expectThat(actual) isSameInstanceAs evaluation
        verify {
            flowFactory.deliverFlow(any())
        }
    }
}
