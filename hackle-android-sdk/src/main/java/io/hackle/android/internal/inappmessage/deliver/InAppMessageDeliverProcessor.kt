package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.activity.lifecycle.ActivityState
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluateResponse
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluator
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.session.SessionUserDecorator
import io.hackle.android.internal.task.*
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.user.decorateWith
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.user.HackleUser
import java.util.concurrent.CompletableFuture

internal class InAppMessageDeliverProcessor(
    private val activityProvider: ActivityProvider,
    private val userManager: UserManager,
    private val sessionUserDecorator: SessionUserDecorator,
    private val identifierChecker: InAppMessageIdentifierChecker,
    private val evaluator: InAppMessageDeliverEvaluator,
    private val presentProcessor: InAppMessagePresentProcessor,
) {

    fun process(request: InAppMessageDeliverRequest): CompletableFuture<InAppMessageDeliverResponse> {
        log.debug { "InAppMessage Deliver Request: $request" }
        return Futures.wrap { deliver(request) }
            .recover {
                log.error { "Failed to process InAppMessageDeliver: $it" }
                InAppMessageDeliverResponse.of(request, Code.EXCEPTION)
            }
            .onSuccess {
                log.debug { "InAppMessage Deliver Response: $it" }
            }
    }

    private fun deliver(request: InAppMessageDeliverRequest): CompletableFuture<InAppMessageDeliverResponse> {

        // check ActivityState
        if (activityProvider.currentState != ActivityState.ACTIVE) {
            val response = InAppMessageDeliverResponse.of(request, Code.ACTIVITY_INACTIVE)
            return response.asFuture()
        }

        // check User
        val user = userManager.hackleUser()
            .decorateWith(sessionUserDecorator)
        val isIdentifierChanged =
            identifierChecker.isIdentifierChanged(request.identifiers, Identifiers.from(user.identifiers))
        if (isIdentifierChanged) {
            val response = InAppMessageDeliverResponse.of(request, Code.IDENTIFIER_CHANGED)
            return response.asFuture()
        }

        return evaluator.evaluate(request, user) // evaluate (dedup + re-evaluate)
            .flatMap { resolve(request, user, it) }
    }

    private fun resolve(
        request: InAppMessageDeliverRequest,
        user: HackleUser,
        response: InAppMessageDeliverEvaluateResponse,
    ): CompletableFuture<InAppMessageDeliverResponse> {
        if (!response.isEligible) {
            return InAppMessageDeliverResponse.of(request, response.code ?: Code.INELIGIBLE).asFuture()
        }

        val evaluation = requireNotNull(response.evaluation) { "evaluation" }

        val presentRequest = InAppMessagePresentRequest.of(request, user, evaluation)
        return presentProcessor.process(presentRequest)
            .map { InAppMessageDeliverResponse.of(request, Code.DELIVER, it) }
    }

    companion object {
        private val log = Logger<InAppMessageDeliverProcessor>()
    }
}
