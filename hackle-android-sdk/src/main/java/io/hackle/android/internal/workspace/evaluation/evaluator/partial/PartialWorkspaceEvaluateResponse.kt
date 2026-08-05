package io.hackle.android.internal.workspace.evaluation.evaluator.partial

import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateResponse
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation

internal class PartialWorkspaceEvaluateResponse(
    override val evaluation: WorkspaceEvaluation,
) : WorkspaceEvaluateResponse
