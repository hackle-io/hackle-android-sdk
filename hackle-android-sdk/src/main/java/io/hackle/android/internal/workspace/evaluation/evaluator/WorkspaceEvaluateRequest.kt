package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateScope
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext

internal interface WorkspaceEvaluateRequest {
    val scope: WorkspaceEvaluateScope
    val context: WorkspaceEvaluationContext
}
