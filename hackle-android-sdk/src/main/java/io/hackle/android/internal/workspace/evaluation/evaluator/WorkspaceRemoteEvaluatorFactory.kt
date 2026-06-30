package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateScope

internal class WorkspaceRemoteEvaluatorFactory(
    private val evaluators: List<WorkspaceRemoteEvaluator<out WorkspaceEvaluateRequest>>,
) {

    fun get(scope: WorkspaceEvaluateScope): WorkspaceRemoteEvaluator<WorkspaceEvaluateRequest> {
        @Suppress("UNCHECKED_CAST")
        val evaluator = evaluators.find { it.supports(scope) } as? WorkspaceRemoteEvaluator<WorkspaceEvaluateRequest>
        return requireNotNull(evaluator) { "Not found WorkspaceRemoteEvaluator (scope=$scope)" }
    }
}
