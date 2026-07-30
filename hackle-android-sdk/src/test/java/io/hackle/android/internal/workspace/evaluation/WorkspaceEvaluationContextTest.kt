package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationContextDto
import io.hackle.android.support.Workspaces
import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class WorkspaceEvaluationContextTest {

    @Test
    fun `keyOf - sessionId와 hackleDeviceId는 키에서 제외한다`() {
        // given
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "id")
            .identifier(IdentifierType.USER, "user")
            .identifier(IdentifierType.DEVICE, "device")
            .identifier(IdentifierType.SESSION, "session")
            .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device")
            .build()

        // when
        val actual = WorkspaceEvaluationContext.keyOf(user)

        // then
        expectThat(actual.identifiers.asMap()) isEqualTo mapOf(
            "\$id" to "id",
            "\$userId" to "user",
            "\$deviceId" to "device",
        )
    }

    @Test
    fun `keyOf - User도 동일한 규칙으로 키를 만든다`() {
        // given
        val user = User.builder()
            .id("id")
            .userId("user")
            .deviceId("device")
            .identifier("\$sessionId", "session")
            .identifier("\$hackleDeviceId", "hackle_device")
            .identifier("custom", "custom_id")
            .build()

        // when
        val actual = WorkspaceEvaluationContext.keyOf(user)

        // then
        expectThat(actual.identifiers.asMap()) isEqualTo mapOf(
            "\$id" to "id",
            "\$userId" to "user",
            "\$deviceId" to "device",
            "custom" to "custom_id",
        )
    }

    @Test
    fun `of - dto로부터 WorkspaceEvaluation을 만든다`() {
        // given
        val key = Workspaces.evaluationKey("\$id" to "user")
        val dto = Workspaces.evaluationDto(
            workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100, modifiedAt = "42")
        )

        // when
        val actual = WorkspaceEvaluationContext.of(key, dto, fullEvaluatedAt = 100)

        // then
        expectThat(actual) {
            get { this.key } isSameInstanceAs key
            get { this.dto } isSameInstanceAs dto
            get { this.fullEvaluatedAt } isEqualTo 100L
            get { this.workspace.metadata.id } isEqualTo 1L
            get { this.workspace.metadata.environmentId } isEqualTo 2L
            get { this.workspace.metadata.evaluatedAt } isEqualTo 100L
            get { this.workspace.metadata.modifiedAt } isEqualTo "42"
        }
    }

    @Test
    fun `from - 저장된 dto로부터 컨텍스트를 복원한다`() {
        // given
        val dto = WorkspaceEvaluationContextDto(
            key = mapOf("\$id" to "user"),
            evaluation = Workspaces.evaluationDto(),
            fullEvaluatedAt = 42
        )

        // when
        val actual = WorkspaceEvaluationContext.from(dto)

        // then
        expectThat(actual) {
            get { key } isEqualTo Workspaces.evaluationKey("\$id" to "user")
            get { fullEvaluatedAt } isEqualTo 42L
        }
    }

    @Test
    fun `toDto - fullEvaluatedAt, metadata, 엔티티 요약을 매핑한다`() {
        // given
        val metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100)
        val context = Workspaces.evaluationContext(
            evaluation = Workspaces.evaluationDto(
                metadata = metadata,
                results = listOf(
                    Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11),
                    Workspaces.resultDto(type = "IN_APP_MESSAGE", id = 2, hash = 22),
                )
            ),
            fullEvaluatedAt = 42
        )

        // when
        val actual = context.toDto()

        // then
        expectThat(actual) {
            get { this.fullEvaluatedAt } isEqualTo 42L
            get { this.metadata } isSameInstanceAs metadata
            get { this.entities.map { Triple(it.type, it.id, it.hash) } } isEqualTo listOf(
                Triple("AB_TEST", 1L, 11),
                Triple("IN_APP_MESSAGE", 2L, 22),
            )
        }
    }
}
