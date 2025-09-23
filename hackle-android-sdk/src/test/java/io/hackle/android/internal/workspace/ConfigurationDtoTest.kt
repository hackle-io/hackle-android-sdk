package io.hackle.android.internal.workspace

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class ConfigurationDtoTest {

    @Test
    fun `ParameterConfigurationDto creation and properties`() {
        val parameterDto1 = ParameterConfigurationDto.ParameterDto(
            key = "theme",
            value = "dark"
        )
        val parameterDto2 = ParameterConfigurationDto.ParameterDto(
            key = "timeout",
            value = 30
        )
        val parameterConfigDto = ParameterConfigurationDto(
            id = 1L,
            parameters = listOf(parameterDto1, parameterDto2)
        )

        expectThat(parameterConfigDto.id).isEqualTo(1L)
        expectThat(parameterConfigDto.parameters).isEqualTo(listOf(parameterDto1, parameterDto2))
    }

    @Test
    fun `ParameterDto creation and properties`() {
        val parameterDto1 = ParameterConfigurationDto.ParameterDto(
            key = "feature_enabled",
            value = true
        )
        val parameterDto2 = ParameterConfigurationDto.ParameterDto(
            key = "max_retry",
            value = 5
        )
        val parameterDto3 = ParameterConfigurationDto.ParameterDto(
            key = "api_endpoint",
            value = "https://api.example.com"
        )

        expectThat(parameterDto1.key).isEqualTo("feature_enabled")
        expectThat(parameterDto1.value).isEqualTo(true)

        expectThat(parameterDto2.key).isEqualTo("max_retry")
        expectThat(parameterDto2.value).isEqualTo(5)

        expectThat(parameterDto3.key).isEqualTo("api_endpoint")
        expectThat(parameterDto3.value).isEqualTo("https://api.example.com")
    }

    @Test
    fun `RemoteConfigParameterDto creation and properties`() {
        val targetDto = TargetDto(conditions = emptyList())
        val valueDto = RemoteConfigParameterDto.ValueDto(
            id = 1L,
            value = "default_value"
        )
        val targetRuleDto = RemoteConfigParameterDto.TargetRuleDto(
            key = "rule1",
            name = "Test Rule",
            target = targetDto,
            bucketId = 100L,
            value = valueDto
        )
        val remoteConfigDto = RemoteConfigParameterDto(
            id = 100L,
            key = "feature_enabled",
            type = "BOOLEAN",
            identifierType = "userId",
            targetRules = listOf(targetRuleDto),
            defaultValue = valueDto
        )

        expectThat(remoteConfigDto.id).isEqualTo(100L)
        expectThat(remoteConfigDto.key).isEqualTo("feature_enabled")
        expectThat(remoteConfigDto.type).isEqualTo("BOOLEAN")
        expectThat(remoteConfigDto.identifierType).isEqualTo("userId")
        expectThat(remoteConfigDto.targetRules).isEqualTo(listOf(targetRuleDto))
        expectThat(remoteConfigDto.defaultValue).isEqualTo(valueDto)
    }

    @Test
    fun `RemoteConfigParameterDto TargetRuleDto creation and properties`() {
        val targetDto = TargetDto(conditions = emptyList())
        val valueDto = RemoteConfigParameterDto.ValueDto(
            id = 2L,
            value = 42
        )
        val targetRuleDto = RemoteConfigParameterDto.TargetRuleDto(
            key = "premium_rule",
            name = "Premium Users Rule",
            target = targetDto,
            bucketId = 200L,
            value = valueDto
        )

        expectThat(targetRuleDto.key).isEqualTo("premium_rule")
        expectThat(targetRuleDto.name).isEqualTo("Premium Users Rule")
        expectThat(targetRuleDto.target).isEqualTo(targetDto)
        expectThat(targetRuleDto.bucketId).isEqualTo(200L)
        expectThat(targetRuleDto.value).isEqualTo(valueDto)
    }

    @Test
    fun `RemoteConfigParameterDto ValueDto creation and properties`() {
        val valueDto1 = RemoteConfigParameterDto.ValueDto(
            id = 10L,
            value = "string_value"
        )
        val valueDto2 = RemoteConfigParameterDto.ValueDto(
            id = 20L,
            value = 123
        )
        val valueDto3 = RemoteConfigParameterDto.ValueDto(
            id = 30L,
            value = true
        )

        expectThat(valueDto1.id).isEqualTo(10L)
        expectThat(valueDto1.value).isEqualTo("string_value")

        expectThat(valueDto2.id).isEqualTo(20L)
        expectThat(valueDto2.value).isEqualTo(123)

        expectThat(valueDto3.id).isEqualTo(30L)
        expectThat(valueDto3.value).isEqualTo(true)
    }

    @Test
    fun `DurationDto creation and properties`() {
        val duration1 = DurationDto(
            timeUnit = "MINUTES",
            amount = 30
        )
        val duration2 = DurationDto(
            timeUnit = "HOURS",
            amount = 24
        )
        val duration3 = DurationDto(
            timeUnit = "DAYS",
            amount = 7
        )

        expectThat(duration1.timeUnit).isEqualTo("MINUTES")
        expectThat(duration1.amount).isEqualTo(30)

        expectThat(duration2.timeUnit).isEqualTo("HOURS")
        expectThat(duration2.amount).isEqualTo(24)

        expectThat(duration3.timeUnit).isEqualTo("DAYS")
        expectThat(duration3.amount).isEqualTo(7)
    }

    @Test
    fun `ContainerDto creation and properties`() {
        val containerGroupDto = ContainerGroupDto(
            id = 1L,
            experiments = listOf(10L, 20L, 30L)
        )
        val containerDto = ContainerDto(
            id = 200L,
            environmentId = 5L,
            bucketId = 1L,
            groups = listOf(containerGroupDto)
        )

        expectThat(containerDto.id).isEqualTo(200L)
        expectThat(containerDto.environmentId).isEqualTo(5L)
        expectThat(containerDto.bucketId).isEqualTo(1L)
        expectThat(containerDto.groups).isEqualTo(listOf(containerGroupDto))
    }

    @Test
    fun `ContainerGroupDto creation and properties`() {
        val containerGroupDto1 = ContainerGroupDto(
            id = 100L,
            experiments = listOf(1L, 2L, 3L)
        )
        val containerGroupDto2 = ContainerGroupDto(
            id = 200L,
            experiments = emptyList()
        )

        expectThat(containerGroupDto1.id).isEqualTo(100L)
        expectThat(containerGroupDto1.experiments).isEqualTo(listOf(1L, 2L, 3L))

        expectThat(containerGroupDto2.id).isEqualTo(200L)
        expectThat(containerGroupDto2.experiments).isEqualTo(emptyList())
    }

    @Test
    fun `ParameterConfigurationDto equals and hashCode`() {
        val parameterDto = ParameterConfigurationDto.ParameterDto("key", "value")

        val config1 = ParameterConfigurationDto(id = 1L, parameters = listOf(parameterDto))
        val config2 = ParameterConfigurationDto(id = 1L, parameters = listOf(parameterDto))
        val config3 = ParameterConfigurationDto(id = 2L, parameters = emptyList())

        expectThat(config1).isEqualTo(config2)
        expectThat(config1.hashCode()).isEqualTo(config2.hashCode())
        expectThat(config1 == config3).isEqualTo(false)
    }

    @Test
    fun `ParameterDto equals and hashCode`() {
        val param1 = ParameterConfigurationDto.ParameterDto("key1", "value1")
        val param2 = ParameterConfigurationDto.ParameterDto("key1", "value1")
        val param3 = ParameterConfigurationDto.ParameterDto("key2", "value2")

        expectThat(param1).isEqualTo(param2)
        expectThat(param1.hashCode()).isEqualTo(param2.hashCode())
        expectThat(param1 == param3).isEqualTo(false)
    }

    @Test
    fun `RemoteConfigParameterDto equals and hashCode`() {
        val targetDto = TargetDto(conditions = emptyList())
        val valueDto = RemoteConfigParameterDto.ValueDto(1L, "value")
        val targetRuleDto = RemoteConfigParameterDto.TargetRuleDto("rule", "Rule", targetDto, 100L, valueDto)

        val remote1 = RemoteConfigParameterDto(
            id = 100L,
            key = "feature",
            type = "BOOLEAN",
            identifierType = "userId",
            targetRules = listOf(targetRuleDto),
            defaultValue = valueDto
        )
        val remote2 = RemoteConfigParameterDto(
            id = 100L,
            key = "feature",
            type = "BOOLEAN",
            identifierType = "userId",
            targetRules = listOf(targetRuleDto),
            defaultValue = valueDto
        )
        val remote3 = RemoteConfigParameterDto(
            id = 200L,
            key = "different_feature",
            type = "STRING",
            identifierType = "deviceId",
            targetRules = emptyList(),
            defaultValue = valueDto
        )

        expectThat(remote1).isEqualTo(remote2)
        expectThat(remote1.hashCode()).isEqualTo(remote2.hashCode())
        expectThat(remote1 == remote3).isEqualTo(false)
    }

    @Test
    fun `ContainerDto equals and hashCode`() {
        val groupDto = ContainerGroupDto(1L, listOf(1L, 2L))

        val container1 = ContainerDto(id = 200L, environmentId = 1L, bucketId = 10L, groups = listOf(groupDto))
        val container2 = ContainerDto(id = 200L, environmentId = 1L, bucketId = 10L, groups = listOf(groupDto))
        val container3 = ContainerDto(id = 300L, environmentId = 2L, bucketId = 20L, groups = emptyList())

        expectThat(container1).isEqualTo(container2)
        expectThat(container1.hashCode()).isEqualTo(container2.hashCode())
        expectThat(container1 == container3).isEqualTo(false)
    }

    @Test
    fun `ContainerGroupDto equals and hashCode`() {
        val group1 = ContainerGroupDto(id = 100L, experiments = listOf(1L, 2L))
        val group2 = ContainerGroupDto(id = 100L, experiments = listOf(1L, 2L))
        val group3 = ContainerGroupDto(id = 200L, experiments = listOf(3L, 4L))

        expectThat(group1).isEqualTo(group2)
        expectThat(group1.hashCode()).isEqualTo(group2.hashCode())
        expectThat(group1 == group3).isEqualTo(false)
    }

    @Test
    fun `ParameterConfigurationDto copy method`() {
        val originalParam = ParameterConfigurationDto.ParameterDto("original", "value")
        val newParam = ParameterConfigurationDto.ParameterDto("modified", "newValue")

        val original = ParameterConfigurationDto(id = 1L, parameters = listOf(originalParam))
        val copied = original.copy(parameters = listOf(newParam))

        expectThat(copied.parameters).isEqualTo(listOf(newParam))
        expectThat(copied.id).isEqualTo(1L)
        expectThat(original.parameters).isEqualTo(listOf(originalParam))
    }

    @Test
    fun `RemoteConfigParameterDto copy method`() {
        val targetDto = TargetDto(conditions = emptyList())
        val originalValue = RemoteConfigParameterDto.ValueDto(1L, "original")
        val newValue = RemoteConfigParameterDto.ValueDto(2L, "modified")
        val targetRuleDto = RemoteConfigParameterDto.TargetRuleDto("rule", "Rule", targetDto, 100L, originalValue)

        val original = RemoteConfigParameterDto(
            id = 100L,
            key = "original_key",
            type = "STRING",
            identifierType = "userId",
            targetRules = listOf(targetRuleDto),
            defaultValue = originalValue
        )
        val copied = original.copy(
            key = "modified_key",
            type = "BOOLEAN",
            defaultValue = newValue
        )

        expectThat(copied.key).isEqualTo("modified_key")
        expectThat(copied.type).isEqualTo("BOOLEAN")
        expectThat(copied.defaultValue).isEqualTo(newValue)
        expectThat(copied.id).isEqualTo(100L)
        expectThat(copied.identifierType).isEqualTo("userId")
        expectThat(original.key).isEqualTo("original_key")
        expectThat(original.type).isEqualTo("STRING")
        expectThat(original.defaultValue).isEqualTo(originalValue)
    }

    @Test
    fun `ContainerDto copy method`() {
        val originalGroup = ContainerGroupDto(1L, listOf(1L, 2L))
        val newGroup = ContainerGroupDto(2L, listOf(3L, 4L))

        val original = ContainerDto(id = 200L, environmentId = 1L, bucketId = 10L, groups = listOf(originalGroup))
        val copied = original.copy(bucketId = 20L, groups = listOf(newGroup))

        expectThat(copied.bucketId).isEqualTo(20L)
        expectThat(copied.groups).isEqualTo(listOf(newGroup))
        expectThat(copied.id).isEqualTo(200L)
        expectThat(copied.environmentId).isEqualTo(1L)
        expectThat(original.bucketId).isEqualTo(10L)
        expectThat(original.groups).isEqualTo(listOf(originalGroup))
    }

    @Test
    fun `ContainerGroupDto copy method`() {
        val original = ContainerGroupDto(id = 100L, experiments = listOf(1L, 2L, 3L))
        val copied = original.copy(experiments = listOf(4L, 5L))

        expectThat(copied.experiments).isEqualTo(listOf(4L, 5L))
        expectThat(copied.id).isEqualTo(100L)
        expectThat(original.experiments).isEqualTo(listOf(1L, 2L, 3L))
    }

    @Test
    fun `ParameterConfigurationDto with empty parameters`() {
        val parameterConfigDto = ParameterConfigurationDto(
            id = 999L,
            parameters = emptyList()
        )

        expectThat(parameterConfigDto.id).isEqualTo(999L)
        expectThat(parameterConfigDto.parameters).isEqualTo(emptyList())
    }

    @Test
    fun `ContainerDto with empty groups`() {
        val containerDto = ContainerDto(
            id = 888L,
            environmentId = 1L,
            bucketId = 999L,
            groups = emptyList()
        )

        expectThat(containerDto.id).isEqualTo(888L)
        expectThat(containerDto.environmentId).isEqualTo(1L)
        expectThat(containerDto.bucketId).isEqualTo(999L)
        expectThat(containerDto.groups).isEqualTo(emptyList())
    }

    @Test
    fun `RemoteConfigParameterDto with empty targetRules`() {
        val valueDto = RemoteConfigParameterDto.ValueDto(1L, "default")
        val remoteConfigDto = RemoteConfigParameterDto(
            id = 777L,
            key = "empty_rules_feature",
            type = "STRING",
            identifierType = "userId",
            targetRules = emptyList(),
            defaultValue = valueDto
        )

        expectThat(remoteConfigDto.id).isEqualTo(777L)
        expectThat(remoteConfigDto.key).isEqualTo("empty_rules_feature")
        expectThat(remoteConfigDto.type).isEqualTo("STRING")
        expectThat(remoteConfigDto.identifierType).isEqualTo("userId")
        expectThat(remoteConfigDto.targetRules).isEqualTo(emptyList())
        expectThat(remoteConfigDto.defaultValue).isEqualTo(valueDto)
    }
}