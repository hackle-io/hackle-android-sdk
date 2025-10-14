package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateProcessor
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateType
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageLayoutResolver
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.ActivityState
import io.hackle.android.internal.session.SessionUserDecorator
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.user.decorateWith
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityRequest
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageDeliverProcessor(
    private val activityProvider: ActivityProvider,
    private val workspaceFetcher: WorkspaceFetcher,
    private val userManager: UserManager,
    private val sessionUserDecorator: SessionUserDecorator,
    private val identifierChecker: InAppMessageIdentifierChecker,
    private val layoutResolver: InAppMessageLayoutResolver,
    private val evaluateProcessor: InAppMessageEvaluateProcessor,
    private val presentProcessor: InAppMessagePresentProcessor,
) {

    fun process(request: InAppMessageDeliverRequest): InAppMessageDeliverResponse {
        log.debug { "InAppMessage Deliver Request: $request" }

        val response = try {
            deliver(request)
        } catch (e: Exception) {
            log.error { "Failed to process InAppMessageDeliver: $e" }
            InAppMessageDeliverResponse.of(request, Code.EXCEPTION)
        }

        log.debug { "InAppMessage Deliver Response: $response" }
        return response
    }

    private fun deliver(request: InAppMessageDeliverRequest): InAppMessageDeliverResponse {
        // check ActivityState
        if (activityProvider.currentState != ActivityState.ACTIVE) {
            return InAppMessageDeliverResponse.of(request, Code.ACTIVITY_INACTIVE)
        }

        // check Workspace
        val workspace = workspaceFetcher.fetch()
            ?: return InAppMessageDeliverResponse.of(request, Code.WORKSPACE_NOT_FOUND)

        // check InAppMessage
        val inAppMessage = workspace.getInAppMessageOrNull(request.inAppMessageKey)
            ?: return InAppMessageDeliverResponse.of(request, Code.IN_APP_MESSAGE_NOT_FOUND)

        // check User
        val user = userManager.resolve(null, HackleAppContext.DEFAULT)
            .decorateWith(sessionUserDecorator)
        val isIdentifierChanged =
            identifierChecker.isIdentifierChanged(request.identifiers, Identifiers.from(user.identifiers))
        if (isIdentifierChanged) {
            return InAppMessageDeliverResponse.of(request, Code.IDENTIFIER_CHANGED)
        }

        // resolve layout
        val layoutEvaluation = layoutResolver.resolve(workspace, inAppMessage, user)

        // check Evaluation (re-evaluate + dedup)
        val eligibilityRequest = InAppMessageEligibilityRequest(workspace, user, inAppMessage, request.requestedAt)
        val eligibilityEvaluation = evaluateProcessor.process(InAppMessageEvaluateType.DELIVER, eligibilityRequest)
        if (!eligibilityEvaluation.isEligible) {
            log.debug { "InAppMessage Deliver Ineligible: dispatchId=${request.dispatchId}, reason=${eligibilityEvaluation.reason}" }
            return InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
        }

        // present
        val presentRequest =
            InAppMessagePresentRequest.of(request, inAppMessage, user, eligibilityEvaluation, layoutEvaluation)
        val presentResponse = presentProcessor.process(presentRequest)

        return InAppMessageDeliverResponse.of(request, Code.PRESENT, presentResponse)
    }

    companion object {
        private val log = Logger<InAppMessageDeliverProcessor>()
    }
}
