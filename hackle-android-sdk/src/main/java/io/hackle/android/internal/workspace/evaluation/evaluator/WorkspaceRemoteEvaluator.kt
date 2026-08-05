package io.hackle.android.internal.workspace.evaluation.evaluator

import java.util.concurrent.CompletableFuture

internal interface WorkspaceRemoteEvaluator<REQUEST : WorkspaceEvaluateRequest, RESPONSE : WorkspaceEvaluateResponse> {
    fun evaluate(request: REQUEST): CompletableFuture<RESPONSE>
}
