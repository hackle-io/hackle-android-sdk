package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluator
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.inappmessage.trigger.InAppMessageIdentifierChecker
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.ActivityState
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class InAppMessageDeliverProcessor(
    private val activityProvider: ActivityProvider,
    private val workspaceFetcher: WorkspaceFetcher,
    private val userManager: UserManager,
    private val identifierChecker: InAppMessageIdentifierChecker,
    private val evaluator: InAppMessageEvaluator,
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
        val user = userManager.resolve(null)
        val isIdentifierChanged =
            identifierChecker.isIdentifierChanged(request.identifiers, Identifiers.from(user.identifiers))
        if (isIdentifierChanged) {
            return InAppMessageDeliverResponse.of(request, Code.IDENTIFIER_CHANGED)
        }

        // check Evaluation
        val evaluation = if (inAppMessage.evaluateContext.atDeliverTime) {
            val evaluation = evaluator.evaluate(workspace, inAppMessage, user, request.requestedAt)
            log.debug { "InAppMessage Re-evaluate: evaluation=$evaluation, request=$request" }
            evaluation
        } else {
            request.evaluation
        }
        if (!evaluation.isEligible) {
            return InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
        }

        val presentRequest = InAppMessagePresentRequest.of(request, workspace, inAppMessage, user, evaluation)
        val presentResponse = presentProcessor.process(presentRequest)

        return InAppMessageDeliverResponse.of(request, Code.PRESENT, presentResponse)
    }

    companion object {
        private val log = Logger<InAppMessageDeliverProcessor>()
    }
}
