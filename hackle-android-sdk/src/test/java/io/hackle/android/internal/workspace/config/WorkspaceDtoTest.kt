package io.hackle.android.internal.workspace.config

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class WorkspaceDtoTest {

    @Test
    fun `WorkspaceConfigRecordDto creation and properties`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )
        val record = WorkspaceConfigRecordDto(
            lastModified = "2023-01-01T00:00:00Z",
            config = workspaceConfigDto
        )

        expectThat(record.lastModified).isEqualTo("2023-01-01T00:00:00Z")
        expectThat(record.config).isEqualTo(workspaceConfigDto)
    }

    @Test
    fun `WorkspaceConfigRecordDto with null lastModified`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )
        val record = WorkspaceConfigRecordDto(
            lastModified = null,
            config = workspaceConfigDto
        )

        expectThat(record.lastModified).isEqualTo(null)
        expectThat(record.config).isEqualTo(workspaceConfigDto)
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
    fun `WorkspaceConfigRecordDto equals and hashCode`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        val record1 = WorkspaceConfigRecordDto("test", workspaceConfigDto)
        val record2 = WorkspaceConfigRecordDto("test", workspaceConfigDto)
        val record3 = WorkspaceConfigRecordDto("different", workspaceConfigDto)

        expectThat(record1).isEqualTo(record2)
        expectThat(record1.hashCode()).isEqualTo(record2.hashCode())
        expectThat(record1 == record3).isEqualTo(false)
    }

    @Test
    fun `WorkspaceConfigRecordDto copy method`() {
        val workspaceDto = WorkspaceDto(
            id = 1L,
            environment = EnvironmentDto(id = 42L)
        )
        val workspaceConfigDto = WorkspaceConfigDto(
            workspace = workspaceDto,
            experiments = emptyList(),
            featureFlags = emptyList(),
            buckets = emptyList(),
            segments = emptyList(),
            containers = emptyList(),
            parameterConfigurations = emptyList(),
            remoteConfigParameters = emptyList(),
            inAppMessages = emptyList()
        )

        val original = WorkspaceConfigRecordDto("original", workspaceConfigDto)
        val copied = original.copy(lastModified = "modified")

        expectThat(copied.lastModified).isEqualTo("modified")
        expectThat(copied.config).isEqualTo(workspaceConfigDto)
        expectThat(original.lastModified).isEqualTo("original")
    }
}
