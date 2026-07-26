package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.internal.workspace.evaluation.model.EntityDto
import io.hackle.android.internal.workspace.evaluation.model.EvaluateResultDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationDeltaDto
import io.hackle.android.support.Workspaces
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class WorkspaceEvaluationsTest {

    @Test
    fun `merge - 변경된 결과는 교체하고 새 결과는 추가한다`() {
        // given
        val result1 = Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11)
        val result2 = Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 22)
        val evaluation = Workspaces.evaluationDto(results = listOf(result1, result2))

        val result2Changed = Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 222)
        val result3Added = Workspaces.resultDto(type = "AB_TEST", id = 3, hash = 33)
        val delta = Workspaces.deltaDto(changed = listOf(result2Changed, result3Added))

        // when
        val actual = WorkspaceEvaluations.merge(evaluation, delta)

        // then
        expectThat(actual.results) isEqualTo listOf(result1, result2Changed, result3Added)
    }

    @Test
    fun `merge - 삭제된 엔티티는 제거한다`() {
        // given
        val result1 = Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11)
        val result2 = Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 22)
        val evaluation = Workspaces.evaluationDto(results = listOf(result1, result2))

        val delta = Workspaces.deltaDto(deleted = listOf(Workspaces.entityDto(type = "AB_TEST", id = 1)))

        // when
        val actual = WorkspaceEvaluations.merge(evaluation, delta)

        // then
        expectThat(actual.results) isEqualTo listOf(result2)
    }

    @Test
    fun `merge - 타입이 다르면 같은 id라도 다른 엔티티로 구분한다`() {
        // given
        val experiment = Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11)
        val inAppMessage = Workspaces.resultDto(type = "IN_APP_MESSAGE", id = 1, hash = 22)
        val evaluation = Workspaces.evaluationDto(results = listOf(experiment, inAppMessage))

        val delta = Workspaces.deltaDto(deleted = listOf(Workspaces.entityDto(type = "AB_TEST", id = 1)))

        // when
        val actual = WorkspaceEvaluations.merge(evaluation, delta)

        // then
        expectThat(actual.results) isEqualTo listOf(inAppMessage)
    }

    @Test
    fun `merge - workspace와 metadata를 delta의 것으로 교체한다`() {
        // given
        val evaluation = Workspaces.evaluationDto(
            workspace = Workspaces.workspaceDto(id = 1),
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100)
        )
        val delta = Workspaces.deltaDto(
            workspace = Workspaces.workspaceDto(id = 1),
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 200)
        )

        // when
        val actual = WorkspaceEvaluations.merge(evaluation, delta)

        // then
        expectThat(actual.workspace) isSameInstanceAs delta.workspace
        expectThat(actual.metadata) isSameInstanceAs delta.metadata
    }

    @Test
    fun `hash - 결과 순서와 무관하게 동일한 해시를 만든다`() {
        // given
        val result1 = Workspaces.resultDto(id = 1, hash = 11)
        val result2 = Workspaces.resultDto(id = 2, hash = 22)

        // when
        val hash1 = WorkspaceEvaluations.hash(listOf(result1, result2))
        val hash2 = WorkspaceEvaluations.hash(listOf(result2, result1))

        // then
        expectThat(hash1) isEqualTo hash2
    }

    @Test
    fun `hash - 개별 해시를 정렬해 누적한 값을 만든다`() {
        expectThat(WorkspaceEvaluations.hash(emptyList())) isEqualTo 1
        expectThat(
            WorkspaceEvaluations.hash(
                listOf(
                    Workspaces.resultDto(id = 1, hash = 2),
                    Workspaces.resultDto(id = 2, hash = 1),
                )
            )
        ) isEqualTo (1 * 31 + 1) * 31 + 2
    }

    @Test
    fun `merge - 서버가 계산한 delta를 병합하면 서버의 전체 평가 결과와 일치한다`() {
        // given
        val base = listOf(
            Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11),
            Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 22),
            Workspaces.resultDto(type = "REMOTE_CONFIG", id = 4, hash = 44),
        )
        val fresh = listOf(
            Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11),        // 변경 없음
            Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 222),       // 변경
            Workspaces.resultDto(type = "FEATURE_FLAG", id = 3, hash = 33),   // 추가
        )                                                                      // REMOTE_CONFIG 4 삭제
        val delta = serverDelta(fresh, base)
        expectThat(delta.changed.map { it.id }) isEqualTo listOf(2L, 3L)
        expectThat(delta.deleted.map { it.id }) isEqualTo listOf(4L)

        // when
        val actual = WorkspaceEvaluations.merge(Workspaces.evaluationDto(results = base), delta)

        // then
        expectThat(actual.results.map { Triple(it.type, it.id, it.hash) })
            .isEqualTo(fresh.map { Triple(it.type, it.id, it.hash) })
        expectThat(WorkspaceEvaluations.hash(actual.results)) isEqualTo delta.metadata.hash
    }

    @Test
    fun `merge - 변경이 없는 delta를 병합해도 해시가 일치한다`() {
        // given
        val base = listOf(
            Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11),
            Workspaces.resultDto(type = "IN_APP_MESSAGE", id = 2, hash = 22),
        )
        val delta = serverDelta(base, base)
        expectThat(delta.changed.map { it.id }) isEqualTo emptyList()
        expectThat(delta.deleted.map { it.id }) isEqualTo emptyList()

        // when
        val actual = WorkspaceEvaluations.merge(Workspaces.evaluationDto(results = base), delta)

        // then
        expectThat(WorkspaceEvaluations.hash(actual.results)) isEqualTo delta.metadata.hash
    }

    @Test
    fun `merge - 모든 결과가 교체된 delta를 병합해도 해시가 일치한다`() {
        // given
        val base = listOf(
            Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11),
            Workspaces.resultDto(type = "AB_TEST", id = 2, hash = 22),
        )
        val fresh = listOf(
            Workspaces.resultDto(type = "FEATURE_FLAG", id = 3, hash = 33),
            Workspaces.resultDto(type = "REMOTE_CONFIG", id = 4, hash = 44),
        )
        val delta = serverDelta(fresh, base)

        // when
        val actual = WorkspaceEvaluations.merge(Workspaces.evaluationDto(results = base), delta)

        // then
        expectThat(actual.results.map { it.id }) isEqualTo listOf(3L, 4L)
        expectThat(WorkspaceEvaluations.hash(actual.results)) isEqualTo delta.metadata.hash
    }

    private fun serverDelta(
        fresh: List<EvaluateResultDto>,
        base: List<EvaluateResultDto>,
    ): WorkspaceEvaluationDeltaDto {
        val baseEntities = base.associateByTo(hashMapOf()) { it.type to it.id }

        val changed = mutableListOf<EvaluateResultDto>()
        for (result in fresh) {
            val baseEntity = baseEntities.remove(result.type to result.id)
            if (result.hash != baseEntity?.hash) {
                changed.add(result)
            }
        }
        val deleted = baseEntities.values.map { EntityDto(it.type, it.id) }

        return Workspaces.deltaDto(
            metadata = Workspaces.evaluationMetadataDto(hash = WorkspaceEvaluations.hash(fresh)),
            changed = changed,
            deleted = deleted
        )
    }
}
