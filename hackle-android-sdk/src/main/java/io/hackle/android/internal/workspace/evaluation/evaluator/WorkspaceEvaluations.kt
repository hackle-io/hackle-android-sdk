package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.EntityDto
import io.hackle.android.internal.workspace.evaluation.model.EvaluateResultDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDeltaDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteEvaluateResult

internal object WorkspaceEvaluations {

    fun merge(evaluation: WorkspaceEvaluationDto, delta: WorkspaceEvaluationDeltaDto): WorkspaceEvaluationDto {
        val results = evaluation.results.associateByTo(LinkedHashMap()) { it.key() }

        for (change in delta.changed) {
            results[change.key()] = change
        }
        for (delete in delta.deleted) {
            results.remove(delete.key())
        }

        return WorkspaceEvaluationDto(delta.workspace, delta.metadata, results.values.toList())
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

internal fun WorkspaceEvaluationDto.merge(delta: WorkspaceEvaluationDeltaDto): WorkspaceEvaluationDto {
    return WorkspaceEvaluations.merge(this, delta)
}
