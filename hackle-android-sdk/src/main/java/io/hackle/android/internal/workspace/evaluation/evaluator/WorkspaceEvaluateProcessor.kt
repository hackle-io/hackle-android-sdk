package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.task.Task

internal class WorkspaceEvaluateProcessor(
    private val evaluatorFactory: WorkspaceRemoteEvaluatorFactory
) {

    fun process(request: WorkspaceEvaluateRequest): Task<WorkspaceEvaluateResponse> {
        val evaluator = evaluatorFactory.get(request.scope)
        return evaluator.evaluate(request)
    }
}
