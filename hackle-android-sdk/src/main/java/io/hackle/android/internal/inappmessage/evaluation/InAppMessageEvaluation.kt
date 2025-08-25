package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluation

internal data class InAppMessageEvaluation(
    val isEligible: Boolean,
    val reason: DecisionReason,
) {

    companion object {
        fun from(evaluation: InAppMessageEligibilityEvaluation): InAppMessageEvaluation {
            return InAppMessageEvaluation(
                isEligible = evaluation.isEligible,
                reason = evaluation.reason
            )
        }
    }
}
