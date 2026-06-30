package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.workspace.WorkspaceRecord
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation

internal class WorkspaceEvaluationRecord(
    val key: WorkspaceEvaluationContext.Key,
    val dto: WorkspaceEvaluationDto,
    val evaluation: WorkspaceEvaluation,
) : WorkspaceRecord {

    override fun workspace(): WorkspaceEvaluation {
        return evaluation
    }

    companion object {
        fun from(key: WorkspaceEvaluationContext.Key, dto: WorkspaceEvaluationDto): WorkspaceEvaluationRecord {
            return WorkspaceEvaluationRecord(key, dto, DefaultWorkspaceEvaluation.from(dto))
        }
    }
}
