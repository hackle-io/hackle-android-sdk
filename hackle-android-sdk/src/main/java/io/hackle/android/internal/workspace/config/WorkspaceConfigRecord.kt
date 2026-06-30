package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.workspace.WorkspaceRecord
import io.hackle.sdk.core.workspace.config.WorkspaceConfig

internal class WorkspaceConfigRecord(
    val lastModified: String?,
    val dto: WorkspaceConfigDto,
    val config: WorkspaceConfig,
) : WorkspaceRecord {
    override fun workspace(): WorkspaceConfig {
        return config
    }

    companion object {
        fun of(dto: WorkspaceConfigDto, lastModified: String?): WorkspaceConfigRecord {
            return WorkspaceConfigRecord(
                lastModified = lastModified,
                dto = dto,
                config = DefaultWorkspaceConfig.from(dto, lastModified)
            )
        }

        fun from(dto: WorkspaceConfigRecordDto): WorkspaceConfigRecord {
            return of(dto.config, dto.lastModified)
        }
    }
}
