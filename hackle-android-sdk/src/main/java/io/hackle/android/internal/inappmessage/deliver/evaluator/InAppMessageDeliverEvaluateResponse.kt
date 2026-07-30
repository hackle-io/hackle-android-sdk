package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse

internal class InAppMessageDeliverEvaluateResponse(
    val isEligible: Boolean,
    val code: InAppMessageDeliverResponse.Code?,
    val evaluation: InAppMessageDeliverEvaluation?,
) {
    companion object {
        fun ineligible(code: InAppMessageDeliverResponse.Code): InAppMessageDeliverEvaluateResponse {
            return InAppMessageDeliverEvaluateResponse(
                isEligible = false,
                code = code,
                evaluation = null
            )
        }

        fun of(evaluation: InAppMessageDeliverEvaluation): InAppMessageDeliverEvaluateResponse {
            if (!evaluation.eligibility.evaluation.result.isEligible) {
                return ineligible(InAppMessageDeliverResponse.Code.INELIGIBLE)
            }
            return InAppMessageDeliverEvaluateResponse(
                isEligible = true,
                code = null,
                evaluation = evaluation
            )
        }
    }
}
