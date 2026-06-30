package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateScope
import java.util.concurrent.CompletableFuture

internal interface WorkspaceRemoteEvaluator<REQUEST : WorkspaceEvaluateRequest> {
    fun supports(scope: WorkspaceEvaluateScope): Boolean
    fun evaluate(request: REQUEST): CompletableFuture<WorkspaceEvaluateResponse>
}
