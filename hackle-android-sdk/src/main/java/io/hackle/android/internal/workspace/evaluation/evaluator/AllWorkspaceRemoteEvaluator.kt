package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.Task
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationRecord
import io.hackle.android.internal.workspace.evaluation.model.*
import io.hackle.android.internal.workspace.evaluation.toDto

internal class AllWorkspaceRemoteEvaluator(
    private val client: WorkspaceRemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<AllWorkspaceEvaluateRequest> {
    override fun supports(scope: WorkspaceEvaluateScope): Boolean {
        return scope == WorkspaceEvaluateScope.ALL
    }

    override fun evaluate(request: AllWorkspaceEvaluateRequest): Task<WorkspaceEvaluateResponse> {
        val requestDto = createRequestDto(request)
        return client.evaluate(requestDto)
            .flatMap { resolveResponse(request, it) }
    }

    private fun createRequestDto(request: AllWorkspaceEvaluateRequest): WorkspaceEvaluateRequestDto {
        val record = request.record
        return if (record != null) request.toAutoDto(record) else request.toFullDto()
    }

    private fun resolveResponse(
        request: AllWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto,
    ): Task<WorkspaceEvaluateResponse> {
        val status = WorkspaceEvaluateStatus.valueOf(response.status)
        return when (status) {
            WorkspaceEvaluateStatus.FULL -> resolveFull(response)
            WorkspaceEvaluateStatus.DELTA -> resolveDelta(request, response)
            WorkspaceEvaluateStatus.NOT_MODIFIED -> resolveNotModified()
        }
    }

    private fun resolveFull(dto: WorkspaceEvaluateResponseDto): Task<WorkspaceEvaluateResponse> {
        val evaluation = requireNotNull(dto.evaluation) { "evaluation" }
        val response = WorkspaceEvaluateResponse.of(WorkspaceEvaluateStatus.FULL, evaluation)
        return Task.succeed(response)
    }

    private fun resolveNotModified(): Task<WorkspaceEvaluateResponse> {
        val response = WorkspaceEvaluateResponse.notModified()
        return Task.succeed(response)
    }

    private fun resolveDelta(
        request: AllWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto,
    ): Task<WorkspaceEvaluateResponse> {
        val requestEvaluation = requireNotNull(request.record?.dto) { "request evaluation" }
        val responseEvaluation = requireNotNull(response.evaluation) { "response evaluation" }

        val mergedEvaluation = WorkspaceEvaluations.merge(requestEvaluation, response)
        val mergedHash = WorkspaceEvaluations.hash(mergedEvaluation.items)

        if (mergedHash != responseEvaluation.metadata.results.hash) {
            val requestDto = request.toFullDto()
            return client.evaluate(requestDto)
                .flatMap { resolveFull(it) }
        }

        val mergedResponse = WorkspaceEvaluateResponse.of(WorkspaceEvaluateStatus.FULL, mergedEvaluation)
        return Task.succeed(mergedResponse)
    }
}

internal class AllWorkspaceEvaluateRequest(
    override val context: WorkspaceEvaluationContext,
    val record: WorkspaceEvaluationRecord?,
) : WorkspaceEvaluateRequest {
    override val scope: WorkspaceEvaluateScope get() = WorkspaceEvaluateScope.ALL
}

private fun AllWorkspaceEvaluateRequest.toFullDto(): WorkspaceEvaluateRequestDto {
    return WorkspaceEvaluateRequestDto(
        scope = WorkspaceEvaluateScope.ALL.name,
        policy = WorkspaceEvaluatePolicy.FORCE_FULL.name,
        context = context.toDto(),
        entities = emptyList(),
        current = null
    )
}

private fun AllWorkspaceEvaluateRequest.toAutoDto(record: WorkspaceEvaluationRecord): WorkspaceEvaluateRequestDto {
    return WorkspaceEvaluateRequestDto(
        scope = WorkspaceEvaluateScope.ALL.name,
        policy = WorkspaceEvaluatePolicy.AUTO.name,
        context = context.toDto(),
        entities = record.dto.items.map { EvaluateEntityDto(it.type, it.id, it.hash) },
        current = record.dto.metadata
    )
}
