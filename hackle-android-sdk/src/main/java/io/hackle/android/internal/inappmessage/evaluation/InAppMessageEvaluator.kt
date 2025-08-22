package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.evaluator.Evaluators
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityRequest
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace

internal class InAppMessageEvaluator(
    private val core: HackleCore,
    private val eligibilityEvaluator: InAppMessageEligibilityEvaluator,
) {

    fun evaluate(
        workspace: Workspace,
        inAppMessage: InAppMessage,
        user: HackleUser,
        timestamp: Long,
    ): InAppMessageEvaluation {
        val request = InAppMessageEligibilityRequest(workspace, user, inAppMessage, timestamp)
        val evaluation = core.evaluate(request, Evaluators.context(), eligibilityEvaluator)
        return InAppMessageEvaluation.from(evaluation)
    }
}
