package io.hackle.android.internal.workspace.dto

import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.workspace.WorkspaceConfigDto
import io.hackle.android.internal.workspace.WorkspaceImpl
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import java.nio.file.Files
import java.nio.file.Paths

internal class ResourcesWorkspaceFetcher(fileName: String) : WorkspaceFetcher {

    private val workspace: Workspace

    init {
        val dto = String(Files.readAllBytes(Paths.get("src/test/resources/$fileName"))).parseJson<WorkspaceConfigDto>()
        workspace = WorkspaceImpl.from(dto)
    }

    override fun fetch(): Workspace {
        return workspace
    }
}
