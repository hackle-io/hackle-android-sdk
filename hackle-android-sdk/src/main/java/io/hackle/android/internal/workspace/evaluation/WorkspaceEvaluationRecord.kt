package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.workspace.WorkspaceRecord
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationRecordDto
import io.hackle.sdk.core.model.Identifiers
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
        fun of(key: WorkspaceEvaluationContext.Key, dto: WorkspaceEvaluationDto): WorkspaceEvaluationRecord {
            return WorkspaceEvaluationRecord(key, dto, DefaultWorkspaceEvaluation.from(dto))
        }

        fun from(dto: WorkspaceEvaluationRecordDto): WorkspaceEvaluationRecord {
            return of(
                key = WorkspaceEvaluationContext.Key(Identifiers.from(dto.key)),
                dto = dto.evaluation
            )
        }
    }
}
