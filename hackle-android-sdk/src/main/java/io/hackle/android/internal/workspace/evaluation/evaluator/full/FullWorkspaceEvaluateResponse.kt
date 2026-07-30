package io.hackle.android.internal.workspace.evaluation.evaluator.full

import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateResponse
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation

internal class FullWorkspaceEvaluateResponse(
    val context: WorkspaceEvaluationContext
) : WorkspaceEvaluateResponse {
    override val evaluation: WorkspaceEvaluation get() = context.workspace
}
