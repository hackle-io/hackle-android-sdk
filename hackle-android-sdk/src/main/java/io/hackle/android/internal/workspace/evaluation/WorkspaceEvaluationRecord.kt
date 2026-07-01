package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.android.internal.workspace.WorkspaceRecord
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationRecordDto
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Identifiers
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation

internal class WorkspaceEvaluationRecord(
    val key: Key,
    val dto: WorkspaceEvaluationDto,
    val evaluation: WorkspaceEvaluation,
) : WorkspaceRecord {

    data class Key(val identifiers: Identifiers)

    override fun workspace(): WorkspaceEvaluation {
        return evaluation
    }

    companion object {

        private val EXCLUDED = setOf(IdentifierType.SESSION.key, IdentifierType.HACKLE_DEVICE_ID.key)

        fun of(key: Key, dto: WorkspaceEvaluationDto): WorkspaceEvaluationRecord {
            return WorkspaceEvaluationRecord(key, dto, DefaultWorkspaceEvaluation.from(dto))
        }

        fun from(dto: WorkspaceEvaluationRecordDto): WorkspaceEvaluationRecord {
            return of(
                key = Key(Identifiers.from(dto.key)),
                dto = dto.evaluation
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
