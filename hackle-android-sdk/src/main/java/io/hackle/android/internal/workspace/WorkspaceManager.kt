package io.hackle.android.internal.workspace

import io.hackle.android.internal.sync.Synchronizer
import io.hackle.android.internal.workspace.repository.WorkspaceConfigRepository
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import java.util.concurrent.atomic.AtomicReference

internal class WorkspaceManager(
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher,
    private val repository: WorkspaceConfigRepository
) : WorkspaceFetcher, Synchronizer {

    private var lastModified: AtomicReference<String?> = AtomicReference()
    private val workspace: AtomicReference<Workspace> = AtomicReference()

    fun initialize() {
        readWorkspaceConfigFromLocal()
    }

    override fun fetch(): Workspace? {
        return workspace.get()
    }

    override fun sync() {
        try {
            val config = httpWorkspaceFetcher.fetchIfModified(lastModified.get())
            if (config != null) {
                setWorkspaceConfig(config)
                repository.set(config)
            }
        } catch (e: Exception) {
            log.error { "Failed to fetch workspace config: $e" }
        }
    }

    private fun setWorkspaceConfig(config: WorkspaceConfig) {
        lastModified.set(config.lastModified)
        workspace.set(WorkspaceImpl.from(config.config))
    }

    private fun readWorkspaceConfigFromLocal() {
        try {
            val config = repository.get()
            if (config != null) {
                setWorkspaceConfig(config)
                log.debug { "Workspace config loaded: [last modified: ${config.lastModified}]" }
            }
        } catch (e: Exception) {
            log.error { "Failed to read workspace config from local: $e" }
        }
    }

    companion object {
        private val log = Logger<WorkspaceManager>()
    }
}
