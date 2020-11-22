package io.hackle.android.internal.workspace

import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal class CachedWorkspaceFetcher(
    private val workspaceCache: WorkspaceCache
) : WorkspaceFetcher {
    override fun fetch(): Workspace? = workspaceCache.get()
}