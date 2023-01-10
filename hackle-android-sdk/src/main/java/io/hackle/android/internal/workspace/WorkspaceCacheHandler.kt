package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger

internal class WorkspaceCacheHandler(
    private val workspaceCache: WorkspaceCache,
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher,
) {

    fun fetchAndCache() {
        try {
            val workspace = httpWorkspaceFetcher.fetch()
            workspaceCache.put(workspace)
        } catch (e: Exception) {
            log.error { "Failed to fetch Workspace: $e" }
        }
    }

    companion object {
        private val log = Logger<WorkspaceCacheHandler>()
    }
}
