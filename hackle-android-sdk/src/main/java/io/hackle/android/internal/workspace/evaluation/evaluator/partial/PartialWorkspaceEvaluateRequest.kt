package io.hackle.android.internal.workspace.evaluation.evaluator.partial

import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.model.EntityDto
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.toDto
import io.hackle.sdk.core.model.Entity

internal class PartialWorkspaceEvaluateRequest(
    override val context: RemoteEvaluateContext,
    val entities: List<Entity>,
) : WorkspaceEvaluateRequest


internal fun PartialWorkspaceEvaluateRequest.toDto(): EntityEvaluateRequestDto {
    return EntityEvaluateRequestDto(
        context = context.toDto(),
        entities = entities.map { EntityDto(it.serviceType.name, it.id) }
    )
}
