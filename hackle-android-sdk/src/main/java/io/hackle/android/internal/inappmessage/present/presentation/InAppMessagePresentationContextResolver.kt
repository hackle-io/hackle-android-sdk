package io.hackle.android.internal.inappmessage.present.presentation

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutRequest
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessagePresentationContextResolver(
    private val core: HackleCore,
    private val layoutEvaluator: InAppMessageLayoutEvaluator,
) {

    fun resolve(request: InAppMessagePresentRequest): InAppMessagePresentationContext {
        val layoutRequest = request.toLayoutRequest()
        val layoutEvaluation = core.evaluate(layoutRequest, Evaluators.context(), layoutEvaluator)
        val presentationContext = InAppMessagePresentationContext.of(request, layoutEvaluation)

        log.debug { "InAppMessage PresentContext resolved: $presentationContext" }
        return presentationContext
    }

    private fun InAppMessagePresentRequest.toLayoutRequest(): InAppMessageLayoutRequest {
        return InAppMessageLayoutRequest(workspace, user, inAppMessage)
    }

    companion object {
        private val log = Logger<InAppMessagePresentationContextResolver>()
    }
}
