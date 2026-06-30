package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.Task
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateScope

internal interface WorkspaceRemoteEvaluator<REQUEST : WorkspaceEvaluateRequest> {
    fun supports(scope: WorkspaceEvaluateScope): Boolean
    fun evaluate(request: REQUEST): Task<WorkspaceEvaluateResponse>
}
