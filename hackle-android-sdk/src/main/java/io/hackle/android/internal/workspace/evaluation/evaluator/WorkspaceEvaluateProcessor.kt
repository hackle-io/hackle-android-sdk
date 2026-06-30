package io.hackle.android.internal.workspace.evaluation.evaluator

import java.util.concurrent.CompletableFuture

internal class WorkspaceEvaluateProcessor(
    private val evaluatorFactory: WorkspaceRemoteEvaluatorFactory
) {

    fun process(request: WorkspaceEvaluateRequest): CompletableFuture<WorkspaceEvaluateResponse> {
        val evaluator = evaluatorFactory.get(request.scope)
        return evaluator.evaluate(request)
    }
}
