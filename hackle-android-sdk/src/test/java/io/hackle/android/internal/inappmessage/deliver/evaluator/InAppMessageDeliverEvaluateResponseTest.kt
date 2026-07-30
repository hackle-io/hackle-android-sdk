package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.support.InAppMessages
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class InAppMessageDeliverEvaluateResponseTest {

    @Test
    fun `ineligible - code와 함께 ineligible 응답을 생성한다`() {
        // when
        val actual = InAppMessageDeliverEvaluateResponse.ineligible(Code.WORKSPACE_NOT_FOUND)

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.WORKSPACE_NOT_FOUND
            get { evaluation }.isNull()
        }
    }

    @Test
    fun `of - 평가 결과가 eligible이면 evaluation을 포함한 eligible 응답을 생성한다`() {
        // given
        val deliverEvaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(),
            layout = InAppMessages.layoutResponse(),
        )

        // when
        val actual = InAppMessageDeliverEvaluateResponse.of(deliverEvaluation)

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo true
            get { code }.isNull()
            get { evaluation } isSameInstanceAs deliverEvaluation
        }
    }

    @Test
    fun `of - 평가 결과가 ineligible이면 INELIGIBLE code의 ineligible 응답을 생성한다`() {
        // given
        val deliverEvaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(
                evaluation = InAppMessages.eligibilityEvaluation(
                    result = InAppMessages.eligibilityResult(isEligible = false)
                )
            ),
            layout = InAppMessages.layoutResponse(),
        )

        // when
        val actual = InAppMessageDeliverEvaluateResponse.of(deliverEvaluation)

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.INELIGIBLE
            get { evaluation }.isNull()
        }
    }
}
