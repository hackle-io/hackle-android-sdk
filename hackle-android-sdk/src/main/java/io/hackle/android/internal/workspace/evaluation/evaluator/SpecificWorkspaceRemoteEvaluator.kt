package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.map
import io.hackle.android.internal.workspace.evaluation.model.*
import io.hackle.sdk.core.model.Entity
import java.util.concurrent.CompletableFuture

internal class SpecificWorkspaceRemoteEvaluator(
    private val client: WorkspaceRemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<SpecificWorkspaceEvaluateRequest> {
    override fun supports(scope: WorkspaceEvaluateScope): Boolean {
        return scope == WorkspaceEvaluateScope.SPECIFIC
    }

    override fun evaluate(request: SpecificWorkspaceEvaluateRequest): CompletableFuture<WorkspaceEvaluateResponse> {
        val requestDto = request.toDto()
        return client.evaluate(requestDto)
            .map { resolveResponse(it) }
    }

    private fun resolveResponse(dto: WorkspaceEvaluateResponseDto): WorkspaceEvaluateResponse {
        val evaluation = requireNotNull(dto.evaluation) { "evaluation" }
        return WorkspaceEvaluateResponse.of(WorkspaceEvaluateStatus.FULL, evaluation)
    }
}

internal class SpecificWorkspaceEvaluateRequest(
    override val context: WorkspaceEvaluateContext,
    val targets: List<Entity>,
) : WorkspaceEvaluateRequest {
    override val scope: WorkspaceEvaluateScope get() = WorkspaceEvaluateScope.SPECIFIC
}

private fun SpecificWorkspaceEvaluateRequest.toDto(): WorkspaceEvaluateRequestDto {
    return WorkspaceEvaluateRequestDto(
        scope = WorkspaceEvaluateScope.SPECIFIC.name,
        policy = WorkspaceEvaluatePolicy.FORCE_FULL.name,
        context = context.toDto(),
        entities = targets.map { EvaluateEntityDto(it.serviceType.name, it.id, null) },
        current = null
    )
}
