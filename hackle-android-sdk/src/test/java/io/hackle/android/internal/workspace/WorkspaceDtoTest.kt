package io.hackle.android.internal.workspace

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class WorkspaceDtoTest {

    @Test
    fun `WorkspaceConfig creation and properties`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            events = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )
        val workspaceConfig = WorkspaceConfig(
            lastModified = "2023-01-01T00:00:00Z",
            config = workspaceConfigDto
        )

        expectThat(workspaceConfig.lastModified).isEqualTo("2023-01-01T00:00:00Z")
        expectThat(workspaceConfig.config).isEqualTo(workspaceConfigDto)
    }

    @Test
    fun `WorkspaceConfig with null lastModified`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            events = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )
        val workspaceConfig = WorkspaceConfig(
            lastModified = null,
            config = workspaceConfigDto
        )

        expectThat(workspaceConfig.lastModified).isEqualTo(null)
        expectThat(workspaceConfig.config).isEqualTo(workspaceConfigDto)
    }

    @Test
    fun `WorkspaceConfigDto creation and properties`() {
        val workspaceDto = WorkspaceDto(
            id = 123L,
            environment = EnvironmentDto(id = 456L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = listOf(),
            featureFlags = listOf(),
            buckets = listOf(),
            events = listOf(),
            segments = listOf(),
            containers = listOf(),
            parameterConfigurations = listOf(),
            remoteConfigParameters = listOf(),
            inAppMessages = listOf()
        )

        expectThat(workspaceConfigDto.workspace).isEqualTo(workspaceDto)
        expectThat(workspaceConfigDto.experiments).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.featureFlags).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.buckets).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.events).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.segments).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.containers).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.parameterConfigurations).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.remoteConfigParameters).isEqualTo(emptyList())
        expectThat(workspaceConfigDto.inAppMessages).isEqualTo(emptyList())
    }

    @Test
    fun `WorkspaceDto creation and properties`() {
        val environmentDto = EnvironmentDto(id = 789L)
        val workspaceDto = WorkspaceDto(
            id = 987L,
            environment = environmentDto
        )

        expectThat(workspaceDto.id).isEqualTo(987L)
        expectThat(workspaceDto.environment).isEqualTo(environmentDto)
    }

    @Test
    fun `EnvironmentDto creation and properties`() {
        val environmentDto = EnvironmentDto(id = 555L)

        expectThat(environmentDto.id).isEqualTo(555L)
    }

    @Test
    fun `WorkspaceConfig equals and hashCode`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            events = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        val config1 = WorkspaceConfig("test", workspaceConfigDto)
        val config2 = WorkspaceConfig("test", workspaceConfigDto)
        val config3 = WorkspaceConfig("different", workspaceConfigDto)

        expectThat(config1).isEqualTo(config2)
        expectThat(config1.hashCode()).isEqualTo(config2.hashCode())
        expectThat(config1 == config3).isEqualTo(false)
    }

    @Test
    fun `WorkspaceConfig copy method`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            events = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        val original = WorkspaceConfig("original", workspaceConfigDto)
        val copied = original.copy(lastModified = "modified")

        expectThat(copied.lastModified).isEqualTo("modified")
        expectThat(copied.config).isEqualTo(workspaceConfigDto)
        expectThat(original.lastModified).isEqualTo("original")
    }
}