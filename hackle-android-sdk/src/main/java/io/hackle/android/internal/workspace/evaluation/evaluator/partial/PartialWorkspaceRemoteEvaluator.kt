package io.hackle.android.internal.workspace.evaluation.evaluator.partial

import io.hackle.android.internal.task.map
import io.hackle.android.internal.workspace.evaluation.DefaultWorkspaceEvaluation
import io.hackle.android.internal.workspace.evaluation.client.RemoteEvaluateClient
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateResponseDto
import java.util.concurrent.CompletableFuture

internal class PartialWorkspaceRemoteEvaluator(
    private val client: RemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<PartialWorkspaceEvaluateRequest, PartialWorkspaceEvaluateResponse> {
    override fun evaluate(request: PartialWorkspaceEvaluateRequest): CompletableFuture<PartialWorkspaceEvaluateResponse> {
        val requestDto = request.toDto()
        return client.evaluateEntities(requestDto)
            .map { resolveResponse(it) }
    }

    private fun resolveResponse(response: EntityEvaluateResponseDto): PartialWorkspaceEvaluateResponse {
        val evaluation = DefaultWorkspaceEvaluation.from(response.evaluation)
        return PartialWorkspaceEvaluateResponse(evaluation)
    }
}
