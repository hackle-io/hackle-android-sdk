package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.task.flatMap
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationRecord
import io.hackle.android.internal.workspace.evaluation.model.*
import java.util.concurrent.CompletableFuture

internal class AllWorkspaceRemoteEvaluator(
    private val client: WorkspaceRemoteEvaluateClient,
) : WorkspaceRemoteEvaluator<AllWorkspaceEvaluateRequest> {
    override fun supports(scope: WorkspaceEvaluateScope): Boolean {
        return scope == WorkspaceEvaluateScope.ALL
    }

    override fun evaluate(request: AllWorkspaceEvaluateRequest): CompletableFuture<WorkspaceEvaluateResponse> {
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
    ): CompletableFuture<WorkspaceEvaluateResponse> {
        val status = WorkspaceEvaluateStatus.valueOf(response.status)
        return when (status) {
            WorkspaceEvaluateStatus.FULL -> resolveFull(response)
            WorkspaceEvaluateStatus.DELTA -> resolveDelta(request, response)
            WorkspaceEvaluateStatus.NOT_MODIFIED -> resolveNotModified()
        }
    }

    private fun resolveFull(dto: WorkspaceEvaluateResponseDto): CompletableFuture<WorkspaceEvaluateResponse> {
        val evaluation = requireNotNull(dto.evaluation) { "evaluation" }
        val response = WorkspaceEvaluateResponse.of(WorkspaceEvaluateStatus.FULL, evaluation)
        return response.asFuture()
    }

    private fun resolveNotModified(): CompletableFuture<WorkspaceEvaluateResponse> {
        val response = WorkspaceEvaluateResponse.notModified()
        return response.asFuture()
    }

    private fun resolveDelta(
        request: AllWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponseDto,
    ): CompletableFuture<WorkspaceEvaluateResponse> {
        val currentEvaluation = requireNotNull(request.record?.dto) { "request evaluation" }
        val responseEvaluation = requireNotNull(response.evaluation) { "response evaluation" }

        val mergedEvaluation = WorkspaceEvaluations.merge(currentEvaluation, response)
        val mergedHash = WorkspaceEvaluations.hash(mergedEvaluation.results)

        if (mergedHash != responseEvaluation.metadata.results.hash) {
            val requestDto = request.toFullDto()
            return client.evaluate(requestDto)
                .flatMap { resolveFull(it) }
        }

        val mergedResponse = WorkspaceEvaluateResponse.of(WorkspaceEvaluateStatus.FULL, mergedEvaluation)
        return mergedResponse.asFuture()
    }
}

internal class AllWorkspaceEvaluateRequest(
    override val context: WorkspaceEvaluateContext,
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
        entities = record.dto.results.map { EvaluateEntityDto(it.type, it.id, it.hash) },
        current = record.dto.metadata
    )
}
