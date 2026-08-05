package io.hackle.android.internal.workspace.evaluation.evaluator.full

import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.task.flatMap
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.client.RemoteEvaluateClient
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluations
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.evaluator.merge
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateResponseDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateStatus
import java.util.concurrent.CompletableFuture

internal class FullWorkspaceRemoteEvaluator(
    private val client: RemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<FullWorkspaceEvaluateRequest, FullWorkspaceEvaluateResponse> {
    override fun evaluate(request: FullWorkspaceEvaluateRequest): CompletableFuture<FullWorkspaceEvaluateResponse> {
        val requestDto = request.toDto()
        return client.evaluateIfModified(requestDto)
            .flatMap { resolveResponse(request, it) }
    }

    private fun resolveResponse(
        request: FullWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto?
    ): CompletableFuture<FullWorkspaceEvaluateResponse> {
        if (response == null) { // NOT_MODIFIED
            val context = requireNotNull(request.base) { "request.base" }
            return FullWorkspaceEvaluateResponse(context).asFuture()
        }
        val status = WorkspaceEvaluateStatus.valueOf(response.status)
        return when (status) {
            WorkspaceEvaluateStatus.FULL -> resolveFull(request, response)
            WorkspaceEvaluateStatus.DELTA -> resolveDelta(request, response)
        }
    }

    private fun resolveFull(
        request: FullWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto
    ): CompletableFuture<FullWorkspaceEvaluateResponse> {
        val evaluation = requireNotNull(response.full) { "response.full" }
        val context = WorkspaceEvaluationContext.of(request.context.key, evaluation, evaluation.metadata.evaluatedAt)
        return FullWorkspaceEvaluateResponse(context).asFuture()
    }

    private fun resolveDelta(
        request: FullWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto
    ): CompletableFuture<FullWorkspaceEvaluateResponse> {
        val base = requireNotNull(request.base) { "request.base" }
        val delta = requireNotNull(response.delta) { "response.delta" }

        val mergedEvaluation = base.dto.merge(delta)
        val mergedHash = WorkspaceEvaluations.hash(mergedEvaluation.results)

        if (mergedHash != delta.metadata.hash) {
            val requestDto = request.toForceFull().toDto()
            return client.evaluateIfModified(requestDto)
                .flatMap { resolveFull(request, requireNotNull(it) { "response" }) }
        }

        val context = WorkspaceEvaluationContext.of(base.key, mergedEvaluation, base.fullEvaluatedAt)
        return FullWorkspaceEvaluateResponse(context).asFuture()
    }
}
