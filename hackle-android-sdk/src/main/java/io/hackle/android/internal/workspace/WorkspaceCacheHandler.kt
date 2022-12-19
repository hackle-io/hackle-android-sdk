package io.hackle.android.internal.workspace

import io.hackle.android.internal.lifecycle.AppInitializeListener
import io.hackle.sdk.core.internal.log.Logger

internal class WorkspaceCacheHandler(
    private val workspaceCache: WorkspaceCache,
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher,
) : AppInitializeListener {

    override fun onInitialized() {
        log.debug { "WorkspaceCacheHandler initialize start" }
        try {
            val workspace = httpWorkspaceFetcher.fetch()
            workspaceCache.put(workspace)
        } catch (e: Exception) {
            log.error { "Failed to fetch Workspace: $e" }
        }
        log.debug { "WorkspaceCacheHandler initialize end" }
    }

    companion object {
        private val log = Logger<WorkspaceCacheHandler>()
    }
}
