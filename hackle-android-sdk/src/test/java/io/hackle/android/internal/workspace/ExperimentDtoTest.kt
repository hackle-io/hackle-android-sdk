package io.hackle.android.internal.workspace

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class ExperimentDtoTest {

    @Test
    fun `ExperimentDto creation and properties`() {
        val userOverrideDto = UserOverrideDto(
            userId = "user1",
            variationId = 100L
        )
        val targetActionDto = TargetActionDto(
            type = "VARIATION",
            variationId = 200L,
            bucketId = null
        )
        val executionDto = ExecutionDto(
            status = "RUNNING",
            version = 1,
            userOverrides = listOf(userOverrideDto),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )
        val variationDto = VariationDto(
            id = 10L,
            key = "variation_key",
            status = "ACTIVE",
            parameterConfigurationId = 5L
        )
        val experimentDto = ExperimentDto(
            id = 1L,
            key = 42L,
            name = "Test Experiment",
            status = "RUNNING",
            version = 2,
            variations = listOf(variationDto),
            execution = executionDto,
            winnerVariationId = null,
            identifierType = "userId",
            containerId = null
        )

        expectThat(experimentDto.id).isEqualTo(1L)
        expectThat(experimentDto.key).isEqualTo(42L)
        expectThat(experimentDto.name).isEqualTo("Test Experiment")
        expectThat(experimentDto.status).isEqualTo("RUNNING")
        expectThat(experimentDto.version).isEqualTo(2)
        expectThat(experimentDto.identifierType).isEqualTo("userId")
        expectThat(experimentDto.variations).isEqualTo(listOf(variationDto))
        expectThat(experimentDto.execution).isEqualTo(executionDto)
        expectThat(experimentDto.winnerVariationId).isEqualTo(null)
        expectThat(experimentDto.containerId).isEqualTo(null)
    }

    @Test
    fun `VariationDto creation and properties`() {
        val variationDto = VariationDto(
            id = 123L,
            key = "test_key",
            status = "ACTIVE",
            parameterConfigurationId = 456L
        )

        expectThat(variationDto.id).isEqualTo(123L)
        expectThat(variationDto.key).isEqualTo("test_key")
        expectThat(variationDto.status).isEqualTo("ACTIVE")
        expectThat(variationDto.parameterConfigurationId).isEqualTo(456L)
    }

    @Test
    fun `ExecutionDto creation and properties`() {
        val userOverrideDto1 = UserOverrideDto(
            userId = "user1",
            variationId = 200L
        )
        val userOverrideDto2 = UserOverrideDto(
            userId = "user2",
            variationId = 300L
        )
        val targetActionDto = TargetActionDto(
            type = "VARIATION",
            variationId = 100L,
            bucketId = null
        )
        val executionDto = ExecutionDto(
            status = "ACTIVE",
            version = 5,
            userOverrides = listOf(userOverrideDto1, userOverrideDto2),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )

        expectThat(executionDto.status).isEqualTo("ACTIVE")
        expectThat(executionDto.version).isEqualTo(5)
        expectThat(executionDto.userOverrides).isEqualTo(listOf(userOverrideDto1, userOverrideDto2))
    }

    @Test
    fun `UserOverrideDto creation and properties`() {
        val userOverrideDto = UserOverrideDto(
            userId = "user123",
            variationId = 999L
        )

        expectThat(userOverrideDto.userId).isEqualTo("user123")
        expectThat(userOverrideDto.variationId).isEqualTo(999L)
    }

    @Test
    fun `ExperimentDto equals and hashCode`() {
        val userOverrideDto = UserOverrideDto(
            userId = "user1",
            variationId = 100L
        )
        val targetActionDto = TargetActionDto(
            type = "VARIATION",
            variationId = 200L,
            bucketId = null
        )
        val executionDto = ExecutionDto(
            status = "RUNNING",
            version = 1,
            userOverrides = listOf(userOverrideDto),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )
        val variationDto = VariationDto(
            id = 10L,
            key = "var_key",
            status = "ACTIVE",
            parameterConfigurationId = 5L
        )

        val experiment1 = ExperimentDto(
            id = 1L,
            key = 42L,
            name = "Test",
            status = "RUNNING",
            version = 1,
            variations = listOf(variationDto),
            execution = executionDto,
            winnerVariationId = null,
            identifierType = "userId",
            containerId = null
        )
        val experiment2 = ExperimentDto(
            id = 1L,
            key = 42L,
            name = "Test",
            status = "RUNNING",
            version = 1,
            variations = listOf(variationDto),
            execution = executionDto,
            winnerVariationId = null,
            identifierType = "userId",
            containerId = null
        )
        val experiment3 = ExperimentDto(
            id = 2L,
            key = 43L,
            name = "Different",
            status = "PAUSED",
            version = 2,
            variations = emptyList(),
            execution = executionDto,
            winnerVariationId = 123L,
            identifierType = "deviceId",
            containerId = 10L
        )

        expectThat(experiment1).isEqualTo(experiment2)
        expectThat(experiment1.hashCode()).isEqualTo(experiment2.hashCode())
        expectThat(experiment1 == experiment3).isEqualTo(false)
    }

    @Test
    fun `VariationDto equals and hashCode`() {
        val variation1 = VariationDto(
            id = 100L,
            key = "key",
            status = "ACTIVE",
            parameterConfigurationId = 50L
        )
        val variation2 = VariationDto(
            id = 100L,
            key = "key",
            status = "ACTIVE",
            parameterConfigurationId = 50L
        )
        val variation3 = VariationDto(
            id = 200L,
            key = "different",
            status = "INACTIVE",
            parameterConfigurationId = 75L
        )

        expectThat(variation1).isEqualTo(variation2)
        expectThat(variation1.hashCode()).isEqualTo(variation2.hashCode())
        expectThat(variation1 == variation3).isEqualTo(false)
    }

    @Test
    fun `ExecutionDto equals and hashCode`() {
        val userOverride = UserOverrideDto("user1", 100L)

        val targetActionDto = TargetActionDto("VARIATION", 100L, null)

        val execution1 = ExecutionDto(
            status = "RUNNING",
            version = 1,
            userOverrides = listOf(userOverride),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )
        val execution2 = ExecutionDto(
            status = "RUNNING",
            version = 1,
            userOverrides = listOf(userOverride),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )
        val execution3 = ExecutionDto(
            status = "PAUSED",
            version = 2,
            userOverrides = emptyList(),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )

        expectThat(execution1).isEqualTo(execution2)
        expectThat(execution1.hashCode()).isEqualTo(execution2.hashCode())
        expectThat(execution1 == execution3).isEqualTo(false)
    }

    @Test
    fun `UserOverrideDto equals and hashCode`() {
        val override1 = UserOverrideDto(
            userId = "user1",
            variationId = 100L
        )
        val override2 = UserOverrideDto(
            userId = "user1",
            variationId = 100L
        )
        val override3 = UserOverrideDto(
            userId = "user2",
            variationId = 200L
        )

        expectThat(override1).isEqualTo(override2)
        expectThat(override1.hashCode()).isEqualTo(override2.hashCode())
        expectThat(override1 == override3).isEqualTo(false)
    }

    @Test
    fun `ExperimentDto copy method`() {
        val targetActionDto = TargetActionDto("VARIATION", 100L, null)
        val executionDto = ExecutionDto(
            status = "RUNNING",
            version = 1,
            userOverrides = emptyList(),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = targetActionDto
        )
        val originalExperiment = ExperimentDto(
            id = 1L,
            key = 42L,
            name = "Original",
            status = "RUNNING",
            version = 1,
            variations = emptyList(),
            execution = executionDto,
            winnerVariationId = null,
            identifierType = "userId",
            containerId = null
        )
        val copiedExperiment = originalExperiment.copy(
            name = "Modified",
            status = "PAUSED"
        )

        expectThat(copiedExperiment.name).isEqualTo("Modified")
        expectThat(copiedExperiment.status).isEqualTo("PAUSED")
        expectThat(copiedExperiment.id).isEqualTo(1L)
        expectThat(copiedExperiment.key).isEqualTo(42L)
        expectThat(originalExperiment.name).isEqualTo("Original")
        expectThat(originalExperiment.status).isEqualTo("RUNNING")
    }

    @Test
    fun `VariationDto copy method`() {
        val originalVariation = VariationDto(
            id = 100L,
            key = "original",
            status = "ACTIVE",
            parameterConfigurationId = 50L
        )
        val copiedVariation = originalVariation.copy(
            key = "modified",
            status = "INACTIVE"
        )

        expectThat(copiedVariation.key).isEqualTo("modified")
        expectThat(copiedVariation.status).isEqualTo("INACTIVE")
        expectThat(copiedVariation.id).isEqualTo(100L)
        expectThat(copiedVariation.parameterConfigurationId).isEqualTo(50L)
        expectThat(originalVariation.key).isEqualTo("original")
        expectThat(originalVariation.status).isEqualTo("ACTIVE")
    }
}