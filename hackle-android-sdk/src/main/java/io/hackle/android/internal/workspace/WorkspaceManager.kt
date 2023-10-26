package io.hackle.android.internal.workspace

import io.hackle.android.internal.sync.Synchronizer
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import java.util.concurrent.atomic.AtomicReference

internal class WorkspaceManager(
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher,
) : WorkspaceFetcher, Synchronizer {

    private val workspace: AtomicReference<Workspace> = AtomicReference()

    override fun fetch(): Workspace? {
        return workspace.get()
    }

    override fun sync() {
        try {
            val workspace = httpWorkspaceFetcher.fetchIfModified()
            if (workspace != null) {
                this.workspace.set(workspace)
            }
        } catch (e: Exception) {
            log.error { "Failed to fetch Workspace: $e" }
        }
    }

    companion object {
        private val log = Logger<WorkspaceManager>()
    }
}
