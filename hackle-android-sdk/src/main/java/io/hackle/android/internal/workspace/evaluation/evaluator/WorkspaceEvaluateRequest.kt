package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateScope

internal interface WorkspaceEvaluateRequest {
    val scope: WorkspaceEvaluateScope
    val context: WorkspaceEvaluateContext
}
