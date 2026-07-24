package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.utils.json.parseJson
import io.hackle.sdk.core.model.Experiment
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Files
import java.nio.file.Paths

class DefaultWorkspaceConfigTest {

    private fun workspace(modifiedAt: String? = null): io.hackle.sdk.core.workspace.config.WorkspaceConfig {
        val body = String(Files.readAllBytes(Paths.get("src/test/resources/workspace_response.json")))
        val dto = body.parseJson<WorkspaceConfigDto>()
        return DefaultWorkspaceConfig.from(dto, modifiedAt)
    }

    @Test
    fun `metadata - workspace id, environmentId, modifiedAt을 매핑한다`() {
        // when
        val actual = workspace(modifiedAt = "42")

        // then
        expectThat(actual.metadata) {
            get { id } isEqualTo 7356L
            get { environmentId } isEqualTo 112712L
            get { modifiedAt } isEqualTo "42"
        }
    }

    @Test
    fun `experiment - 실행 상태를 매핑하고 key로 조회한다`() {
        // when
        val actual = workspace()

        // then
        expectThat(actual.experiments).hasSize(6)
        expectThat(actual.getExperimentOrNull(5)).isNotNull().and {
            get { key } isEqualTo 5L
            get { order } isEqualTo 5L
            get { type } isEqualTo Experiment.Type.AB_TEST
            get { status } isEqualTo Experiment.Status.DRAFT
        }
        expectThat(actual.getExperimentOrNull(4)).isNotNull()
            .get { status } isEqualTo Experiment.Status.COMPLETED
        expectThat(actual.getExperimentOrNull(999)).isNull()
    }

    @Test
    fun `featureFlag - 실행 상태를 매핑하고 key로 조회한다`() {
        // when
        val actual = workspace()

        // then
        expectThat(actual.featureFlags).hasSize(4)
        expectThat(actual.getFeatureFlagOrNull(1)).isNotNull().and {
            get { type } isEqualTo Experiment.Type.FEATURE_FLAG
            get { status } isEqualTo Experiment.Status.RUNNING
        }
        expectThat(actual.getFeatureFlagOrNull(3)).isNotNull()
            .get { status } isEqualTo Experiment.Status.PAUSED
        expectThat(actual.getFeatureFlagOrNull(999)).isNull()
    }

    @Test
    fun `component - bucket, segment, container를 조회한다`() {
        // when
        val actual = workspace()

        // then
        expectThat(actual.getBucketOrNull(228117)).isNotNull()
            .get { id } isEqualTo 228117L
        expectThat(actual.getBucketOrNull(999)).isNull()
        expectThat(actual.getSegmentOrNull("testdwdskladmksalda")).isNotNull()
            .get { id } isEqualTo 100431L
        expectThat(actual.getSegmentOrNull("!!")).isNull()
        expectThat(actual.getContainerOrNull(999)).isNull()
    }

    @Test
    fun `experiment - order 순서로 정렬한다`() {
        // given
        val dto = WorkspaceConfigDto(
            workspace = WorkspaceDto(id = 1, environment = EnvironmentDto(id = 2)),
            experiments = listOf(
                experimentDto(id = 1, key = 1, order = 2),
                experimentDto(id = 2, key = 2, order = 1),
            ),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        // when
        val actual = DefaultWorkspaceConfig.from(dto, null)

        // then
        expectThat(actual.experiments.map { it.key }) isEqualTo listOf(2L, 1L)
    }

    @Test
    fun `experiment - variation의 parameterConfiguration을 id로 찾아 매핑한다`() {
        // given
        val dto = WorkspaceConfigDto(
            workspace = WorkspaceDto(id = 1, environment = EnvironmentDto(id = 2)),
            experiments = listOf(
                experimentDto(
                    id = 1,
                    key = 1,
                    variations = listOf(
                        VariationDto(id = 1, key = "A", status = "ACTIVE", parameterConfigurationId = 320),
                        VariationDto(id = 2, key = "B", status = "DROPPED", parameterConfigurationId = null),
                    )
                ),
            ),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = listOf(
                ParameterConfigurationDto(
                    id = 320,
                    parameters = listOf(ParameterConfigurationDto.ParameterDto("color", "blue"))
                )
            ),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        // when
        val actual = DefaultWorkspaceConfig.from(dto, null)

        // then
        val variations = actual.getExperimentOrNull(1)!!.variations
        expectThat(variations[0]) {
            get { parameterConfiguration }.isNotNull()
                .get { parameters } isEqualTo mapOf<String, Any>("color" to "blue")
            get { isDropped } isEqualTo false
        }
        expectThat(variations[1]) {
            get { parameterConfiguration }.isNull()
            get { isDropped } isEqualTo true
        }
    }

    @Test
    fun `experiment - 지원하지 않는 실행 상태면 제외한다`() {
        // given
        val dto = WorkspaceConfigDto(
            workspace = WorkspaceDto(id = 1, environment = EnvironmentDto(id = 2)),
            experiments = listOf(
                experimentDto(id = 1, key = 1, executionStatus = "INVALID"),
                experimentDto(id = 2, key = 2, executionStatus = "RUNNING"),
            ),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        // when
        val actual = DefaultWorkspaceConfig.from(dto, null)

        // then
        expectThat(actual.experiments).hasSize(1)
        expectThat(actual.getExperimentOrNull(1)).isNull()
        expectThat(actual.getExperimentOrNull(2)).isNotNull()
    }

    @Test
    fun `toProperties - config_modified_at을 포함한다`() {
        expectThat(workspace(modifiedAt = "42").toProperties()) isEqualTo mapOf<String, Any>("config_modified_at" to "42")
        expectThat(workspace(modifiedAt = null).toProperties()) isEqualTo emptyMap()
    }

    private fun experimentDto(
        id: Long,
        key: Long,
        order: Long = id,
        executionStatus: String = "RUNNING",
        variations: List<VariationDto> = listOf(
            VariationDto(id = 1, key = "A", status = "ACTIVE", parameterConfigurationId = null)
        ),
    ): ExperimentDto {
        return ExperimentDto(
            id = id,
            key = key,
            order = order,
            name = null,
            status = executionStatus,
            version = 1,
            variations = variations,
            execution = ExecutionDto(
                status = executionStatus,
                version = 1,
                userOverrides = emptyList(),
                segmentOverrides = emptyList(),
                targetAudiences = emptyList(),
                targetRules = emptyList(),
                defaultRule = TargetActionDto(type = "VARIATION", variationId = 1, bucketId = null)
            ),
            winnerVariationId = null,
            identifierType = "\$id",
            containerId = null
        )
    }
}
