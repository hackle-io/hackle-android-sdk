package io.hackle.android.internal.workspace.evaluation.evaluator.full

import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationContext
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluatePolicy
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.toDto
import io.hackle.android.internal.workspace.evaluation.toDto

internal class FullWorkspaceEvaluateRequest(
    override val context: RemoteEvaluateContext,
    val policy: WorkspaceEvaluatePolicy,
    val base: WorkspaceEvaluationContext?,
) : WorkspaceEvaluateRequest {
    companion object {
        fun of(context: RemoteEvaluateContext, base: WorkspaceEvaluationContext?): FullWorkspaceEvaluateRequest {
            val policy = if (base == null) WorkspaceEvaluatePolicy.FORCE_FULL else WorkspaceEvaluatePolicy.AUTO
            return FullWorkspaceEvaluateRequest(context, policy, base)
        }
    }
}

internal fun FullWorkspaceEvaluateRequest.toForceFull(): FullWorkspaceEvaluateRequest {
    if (policy == WorkspaceEvaluatePolicy.FORCE_FULL) {
        return this
    }
    return FullWorkspaceEvaluateRequest(
        context = context,
        policy = WorkspaceEvaluatePolicy.FORCE_FULL,
        base = null
    )
}

internal fun FullWorkspaceEvaluateRequest.toDto(): WorkspaceEvaluateRequestDto {
    return WorkspaceEvaluateRequestDto(
        policy = policy.name,
        context = context.toDto(),
        base = base?.toDto()
    )
}
