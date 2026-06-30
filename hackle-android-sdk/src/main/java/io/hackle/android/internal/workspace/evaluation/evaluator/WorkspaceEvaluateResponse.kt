package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateStatus
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto

internal class WorkspaceEvaluateResponse(
    val status: WorkspaceEvaluateStatus,
    val evaluation: WorkspaceEvaluationDto?,
) {
    companion object {
        private val NOT_MODIFIED = WorkspaceEvaluateResponse(WorkspaceEvaluateStatus.NOT_MODIFIED, null)

        fun notModified(): WorkspaceEvaluateResponse {
            return NOT_MODIFIED
        }

        fun of(status: WorkspaceEvaluateStatus, dto: WorkspaceEvaluationDto): WorkspaceEvaluateResponse {
            return WorkspaceEvaluateResponse(status, dto)
        }
    }
}
