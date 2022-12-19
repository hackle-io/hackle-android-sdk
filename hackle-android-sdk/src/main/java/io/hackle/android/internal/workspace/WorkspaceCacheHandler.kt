package io.hackle.android.internal.workspace

import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.ExecutorService

internal class WorkspaceCacheHandler(
    private val executor: ExecutorService,
    private val workspaceCache: WorkspaceCache,
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher
) {

    fun fetchAndCache(onCompleted: () -> Unit) {
        executor.submit {
            try {
                val workspace = httpWorkspaceFetcher.fetch()
                workspaceCache.put(workspace)
            } catch (e: Exception) {
                log.error { "Failed to fetch Workspace: $e" }
            } finally {
                onCompleted()
            }
        }
    }

    companion object {
        private val log = Logger<WorkspaceCacheHandler>()
    }
}
