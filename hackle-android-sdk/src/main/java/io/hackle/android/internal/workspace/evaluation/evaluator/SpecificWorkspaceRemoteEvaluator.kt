package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.Task
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.model.*
import io.hackle.android.internal.workspace.evaluation.toDto
import io.hackle.sdk.core.model.Entity

internal class SpecificWorkspaceRemoteEvaluator(
    private val client: WorkspaceRemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<SpecificWorkspaceEvaluateRequest> {
    override fun supports(scope: WorkspaceEvaluateScope): Boolean {
        return scope == WorkspaceEvaluateScope.SPECIFIC
    }

    override fun evaluate(request: SpecificWorkspaceEvaluateRequest): Task<WorkspaceEvaluateResponse> {
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
    override val context: WorkspaceEvaluationContext,
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
