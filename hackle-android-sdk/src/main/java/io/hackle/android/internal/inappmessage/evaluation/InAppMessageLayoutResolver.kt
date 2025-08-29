package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluation
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutRequest
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageLayoutResolver(
    private val core: HackleCore,
    private val layoutEvaluator: InAppMessageLayoutEvaluator,
) {

    fun resolve(workspace: Workspace, inAppMessage: InAppMessage, user: HackleUser): InAppMessageLayoutEvaluation {
        val request = InAppMessageLayoutRequest(workspace, user, inAppMessage)
        return core.inAppMessage(request, Evaluators.context(), layoutEvaluator)
    }
}