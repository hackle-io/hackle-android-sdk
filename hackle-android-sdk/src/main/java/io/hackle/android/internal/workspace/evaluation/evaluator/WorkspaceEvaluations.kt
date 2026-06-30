package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.EntityDto
import io.hackle.android.internal.workspace.evaluation.model.EvaluateResultDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateResponseDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteEvaluateResult

internal object WorkspaceEvaluations {

    fun merge(evaluation: WorkspaceEvaluationDto, response: WorkspaceEvaluateResponseDto): WorkspaceEvaluationDto {
        val merged = evaluation.items.associateByTo(LinkedHashMap()) { it.key() }

        val responseEvaluation = requireNotNull(response.evaluation) { "evaluation" }
        for (result in responseEvaluation.items) {
            merged[result.key()] = result
        }

        for (entity in response.deleted) {
            merged.remove(entity.key())
        }

        return WorkspaceEvaluationDto(evaluation.workspace, merged.values.toList(), responseEvaluation.metadata)
    }

    fun hash(results: List<EvaluateResultDto>): Int {
        var acc = 1
        for (h in results.map { it.hash }.sorted()) {
            acc = acc * 31 + h
        }
        return acc
    }

    private fun EvaluateResultDto.key(): RemoteEvaluateResult.Key {
        return RemoteEvaluateResult.Key(type, id)
    }

    private fun EntityDto.key(): RemoteEvaluateResult.Key {
        return RemoteEvaluateResult.Key(type, id)
    }
}
