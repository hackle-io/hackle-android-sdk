package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.EvaluationEventRecorder
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.*

internal class InAppMessageEvaluateProcessor(
    private val core: HackleCore,
    private val flowFactory: InAppMessageEligibilityFlowFactory,
    private val eventRecorder: EvaluationEventRecorder,
) {

    fun process(
        type: InAppMessageEvaluateType,
        request: InAppMessageEligibilityRequest,
    ): InAppMessageEligibilityEvaluation {
        val flow = resolveFlow(type, request)
        val evaluator = InAppMessageEligibilityEvaluator(flow, eventRecorder)
        return core.inAppMessage(request, Evaluators.context(), evaluator)
    }

    private fun resolveFlow(
        type: InAppMessageEvaluateType,
        request: InAppMessageEligibilityRequest,
    ): InAppMessageEligibilityFlow {
        return when (type) {
            InAppMessageEvaluateType.TRIGGER -> flowFactory.triggerFlow()
            InAppMessageEvaluateType.DELIVER -> flowFactory.deliverFlow(request.inAppMessage.evaluateContext.atDeliverTime)
        }
    }
}
