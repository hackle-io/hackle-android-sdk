package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext

internal interface WorkspaceEvaluateRequest {
    val context: RemoteEvaluateContext
}
