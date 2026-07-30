package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.sdk.common.PropertiesBuilder
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateResponse

internal data class InAppMessageDeliverEvaluation(
    val eligibility: InAppMessageEligibilityEvaluateResponse,
    val layout: InAppMessageLayoutEvaluateResponse,
) {

    fun toProperties(): Map<String, Any> {
        val experimentEvaluation = layout.experiment ?: return emptyMap()
        return PropertiesBuilder()
            .add("experiment_id", experimentEvaluation.entity.id)
            .add("experiment_key", experimentEvaluation.entity.key)
            .add("variation_id", experimentEvaluation.result.variation.id)
            .add("variation_key", experimentEvaluation.result.variation.key)
            .add("experiment_decision_reason", experimentEvaluation.result.reason.name)
            .build()
    }
}
