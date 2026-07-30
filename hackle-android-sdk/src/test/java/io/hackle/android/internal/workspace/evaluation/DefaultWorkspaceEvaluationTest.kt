package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.workspace.config.RemoteConfigParameterDto
import io.hackle.android.support.Workspaces
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.*
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*

class DefaultWorkspaceEvaluationTest {

    @Test
    fun `from - metadata를 매핑한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100, modifiedAt = "42")
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 99)

        // then
        expectThat(actual.metadata) {
            get { id } isEqualTo 1L
            get { environmentId } isEqualTo 2L
            get { evaluatedAt } isEqualTo 100L
            get { modifiedAt } isEqualTo "42"
        }
        expectThat(actual.fullEvaluatedAt) isEqualTo 99L
    }

    @Test
    fun `from - 결과를 서비스 타입별로 분류하고 order로 정렬한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "AB_TEST", id = 2,
                    experiment = Workspaces.experimentResultDto(id = 2, key = 2, order = 2)
                ),
                Workspaces.resultDto(
                    type = "AB_TEST", id = 1,
                    experiment = Workspaces.experimentResultDto(id = 1, key = 1, order = 1)
                ),
                Workspaces.resultDto(
                    type = "FEATURE_FLAG", id = 3,
                    featureFlag = Workspaces.experimentResultDto(id = 3, key = 3)
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.experiments.map { it.key }) isEqualTo listOf(1L, 2L)
        expectThat(actual.featureFlags).hasSize(1)
        expectThat(actual.getExperimentOrNull(1)).isNotNull().and {
            get { type } isEqualTo Experiment.Type.AB_TEST
            get { status } isEqualTo Experiment.Status.RUNNING
            get { reason } isEqualTo DecisionReason.TRAFFIC_ALLOCATED
        }
        expectThat(actual.getFeatureFlagOrNull(3)).isNotNull()
            .get { type } isEqualTo Experiment.Type.FEATURE_FLAG
        expectThat(actual.getExperimentOrNull(999)).isNull()
    }

    @Test
    fun `from - 알 수 없는 서비스 타입과 payload 없는 결과는 제외한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(type = "UNKNOWN_TYPE", id = 1),
                Workspaces.resultDto(type = "AB_TEST", id = 2, experiment = null),
                Workspaces.resultDto(
                    type = "AB_TEST", id = 3,
                    experiment = Workspaces.experimentResultDto(id = 3, key = 3)
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.experiments).hasSize(1)
        expectThat(actual.experiments[0].key) isEqualTo 3L
    }

    @Test
    fun `from - 지원하지 않는 실행 상태의 experiment는 제외한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "AB_TEST", id = 1,
                    experiment = Workspaces.experimentResultDto(id = 1, key = 1, executionStatus = "INVALID")
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.experiments).hasSize(0)
    }

    @Test
    fun `from - references에서 알 수 없는 타입은 제외한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "AB_TEST", id = 1,
                    experiment = Workspaces.experimentResultDto(
                        id = 1,
                        references = listOf(
                            Workspaces.entityDto(type = "AB_TEST", id = 42),
                            Workspaces.entityDto(type = "UNKNOWN_TYPE", id = 43),
                        )
                    )
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.experiments[0].references) isEqualTo listOf(
            DefaultEntity(ServiceType.AB_TEST, 42)
        )
    }

    @Test
    fun `from - remoteConfig 결과를 매핑한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "REMOTE_CONFIG", id = 1,
                    remoteConfig = Workspaces.remoteConfigResultDto(
                        id = 1,
                        key = "remote_config",
                        valueType = "STRING",
                        value = RemoteConfigParameterDto.ValueDto(id = 320, value = "value"),
                        reason = "TARGET_RULE_MATCH"
                    )
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.getRemoteConfigParameterOrNull("remote_config")).isNotNull().and {
            get { id } isEqualTo 1L
            get { type } isEqualTo ValueType.STRING
            get { value }.isNotNull().and {
                get { id } isEqualTo 320L
                get { rawValue } isEqualTo "value"
            }
            get { reason } isEqualTo DecisionReason.TARGET_RULE_MATCH
        }
        expectThat(actual.getRemoteConfigParameterOrNull("!!")).isNull()
    }

    @Test
    fun `from - 지원하지 않는 valueType의 remoteConfig 결과는 제외한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "REMOTE_CONFIG", id = 1,
                    remoteConfig = Workspaces.remoteConfigResultDto(id = 1, valueType = "INVALID")
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.remoteConfigParameters).hasSize(0)
    }

    @Test
    fun `from - inAppMessage 결과를 layout과 함께 매핑한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "IN_APP_MESSAGE", id = 1,
                    inAppMessage = Workspaces.inAppMessageResultDto(
                        id = 1,
                        key = 320,
                        order = 2,
                        isEligible = true,
                        reason = "IN_APP_MESSAGE_TARGET"
                    )
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.getInAppMessageOrNull(320)).isNotNull().and {
            get { id } isEqualTo 1L
            get { key } isEqualTo 320L
            get { order } isEqualTo 2L
            get { isEligible } isEqualTo true
            get { reason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
            // period, timetable, delay, evaluateContext가 없으면 기본값으로 매핑한다
            get { period } isEqualTo InAppMessage.Period.Always
            get { timetable } isEqualTo InAppMessage.Timetable.All
            get { eventTrigger.delay.type } isEqualTo InAppMessage.Delay.Type.IMMEDIATE
            get { evaluateContext.atDeliverTime } isEqualTo false
            get { layout.message.layout.displayType } isEqualTo InAppMessage.DisplayType.MODAL
            get { layout.reason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
        }
        expectThat(actual.getInAppMessageOrNull(999)).isNull()
    }

    @Test
    fun `from - messageContext 매핑에 실패한 inAppMessage 결과는 제외한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "IN_APP_MESSAGE", id = 1,
                    inAppMessage = Workspaces.inAppMessageResultDto(
                        id = 1,
                        messageContext = Workspaces.messageContextDto(platformTypes = listOf("INVALID"))
                    )
                ),
            )
        )

        // when
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // then
        expectThat(actual.inAppMessages).hasSize(0)
    }

    @Test
    fun `result - 엔티티의 서비스 타입과 id로 결과를 찾는다`() {
        // given
        val dto = Workspaces.evaluationDto(
            results = listOf(
                Workspaces.resultDto(
                    type = "AB_TEST", id = 1,
                    experiment = Workspaces.experimentResultDto(id = 1, key = 1)
                ),
            )
        )
        val actual = DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 42)

        // when & then
        expectThat(actual.result(DefaultEntity(ServiceType.AB_TEST, 1))) isSameInstanceAs actual.experiments[0]
        expectThat(actual.result(DefaultEntity(ServiceType.AB_TEST, 999))).isNull()
        expectThat(actual.result(DefaultEntity(ServiceType.IN_APP_MESSAGE, 1))).isNull()
    }

    @Test
    fun `toProperties - 평가 시각 정보를 포함한다`() {
        // given
        val dto = Workspaces.evaluationDto(
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100, modifiedAt = "42")
        )

        // when & then
        expectThat(DefaultWorkspaceEvaluation.from(dto, fullEvaluatedAt = 99).toProperties()) isEqualTo mapOf(
            "config_modified_at" to "42",
            "remote_evaluated_at" to 100L,
            "remote_full_evaluated_at" to 99L,
        )
    }

    @Test
    fun `toProperties - partial 평가 결과에는 full 평가 시각이 없다`() {
        // given
        val dto = Workspaces.entityEvaluationDto(
            metadata = Workspaces.entityMetadataDto(evaluatedAt = 100, modifiedAt = "42")
        )

        // when & then
        expectThat(DefaultWorkspaceEvaluation.from(dto).toProperties()) isEqualTo mapOf(
            "config_modified_at" to "42",
            "remote_evaluated_at" to 100L,
        )
    }
}
