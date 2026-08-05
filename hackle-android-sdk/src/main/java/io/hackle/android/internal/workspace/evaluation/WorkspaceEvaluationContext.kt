package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.android.internal.workspace.WorkspaceContext
import io.hackle.android.internal.workspace.evaluation.model.BaseEvaluationDto
import io.hackle.android.internal.workspace.evaluation.model.EvaluateEntityDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationContextDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation

internal class WorkspaceEvaluationContext(
    override val workspace: WorkspaceEvaluation,
    val key: Key,
    val dto: WorkspaceEvaluationDto,
    val fullEvaluatedAt: Long
) : WorkspaceContext {

    data class Key(val identifiers: Identifiers)

    companion object {

        private val EXCLUDED = setOf(IdentifierType.SESSION.key, IdentifierType.HACKLE_DEVICE_ID.key)

        fun of(key: Key, dto: WorkspaceEvaluationDto, fullEvaluatedAt: Long): WorkspaceEvaluationContext {
            return WorkspaceEvaluationContext(
                workspace = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt),
                key = key,
                dto = dto,
                fullEvaluatedAt = fullEvaluatedAt
            )
        }

        fun from(dto: WorkspaceEvaluationContextDto): WorkspaceEvaluationContext {
            return of(
                key = Key(Identifiers.from(dto.key)),
                dto = dto.evaluation,
                fullEvaluatedAt = dto.fullEvaluatedAt
            )
        }

        fun keyOf(user: HackleUser): Key {
            return Key(Identifiers.from(user.identifiers.filterKeys { it !in EXCLUDED }))
        }

        fun keyOf(user: User): Key {
            return Key(Identifiers.from(user.resolvedIdentifiers.asMap().filterKeys { it !in EXCLUDED }))
        }
    }
}

internal fun WorkspaceEvaluationContext.toDto(): BaseEvaluationDto {
    return BaseEvaluationDto(
        fullEvaluatedAt = fullEvaluatedAt,
        metadata = dto.metadata,
        entities = dto.results.map { EvaluateEntityDto(it.type, it.id, it.hash) }
    )
}
