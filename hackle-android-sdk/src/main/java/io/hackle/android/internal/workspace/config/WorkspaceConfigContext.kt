package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.workspace.WorkspaceContext
import io.hackle.sdk.core.workspace.config.WorkspaceConfig

internal class WorkspaceConfigContext(
    override val workspace: WorkspaceConfig,
    val modifiedAt: String?,
    val dto: WorkspaceConfigDto,
) : WorkspaceContext {


    companion object {
        fun of(dto: WorkspaceConfigDto, modifiedAt: String?): WorkspaceConfigContext {
            return WorkspaceConfigContext(
                workspace = DefaultWorkspaceConfig.from(dto, modifiedAt),
                modifiedAt = modifiedAt,
                dto = dto,
            )
        }

        fun from(dto: WorkspaceConfigRecordDto): WorkspaceConfigContext {
            return of(dto.config, dto.lastModified)
        }
    }
}
