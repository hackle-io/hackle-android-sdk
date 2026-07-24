package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.utils.json.parseJson
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.WorkspaceConfigFetcher
import java.nio.file.Files
import java.nio.file.Paths

internal class ResourcesWorkspaceFetcher(fileName: String) : WorkspaceConfigFetcher {

    private val workspace: WorkspaceConfig

    init {
        val dto = String(Files.readAllBytes(Paths.get("src/test/resources/$fileName"))).parseJson<WorkspaceConfigDto>()
        workspace = DefaultWorkspaceConfig.from(dto, null)
    }

    fun fetch(): WorkspaceConfig {
        return workspace
    }

    override fun metadata(): Workspace.Metadata {
        return workspace.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceConfig {
        return workspace
    }
}
