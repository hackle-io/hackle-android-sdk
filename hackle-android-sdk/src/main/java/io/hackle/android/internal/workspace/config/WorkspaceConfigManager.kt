package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.sync.Synchronizer
import io.hackle.android.internal.task.consume
import io.hackle.android.internal.task.recover
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.WorkspaceConfigFetcher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

internal class WorkspaceConfigManager(
    private val fetcher: HttpWorkspaceConfigFetcher,
    private val repository: WorkspaceConfigRepository
) : WorkspaceManager, WorkspaceConfigFetcher, Synchronizer {

    private val context: AtomicReference<WorkspaceConfigContext?> = AtomicReference()

    override fun initialize() {
        load()
    }

    override fun metadata(): Workspace.Metadata? {
        return context.get()?.workspace?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceConfig? {
        return context.get()?.workspace
    }

    override fun sync(): CompletableFuture<Void> {
        val lastModified = context.get()?.modifiedAt
        return fetcher.fetchIfModified(lastModified)
            .consume { store(it) }
            .recover { log.error { "Failed to fetch WorkspaceConfig: $it" } }
    }

    private fun store(context: WorkspaceConfigContext?) {
        if (context == null) {
            return
        }
        this.context.set(context)
        this.repository.set(context)
    }

    private fun load() {
        try {
            val context = repository.get()
            if (context != null) {
                this.context.set(context)
                log.debug { "WorkspaceConfig loaded: [modifiedAt: ${context.modifiedAt}]" }
            }
        } catch (e: Exception) {
            log.error { "Failed to read WorkspaceConfig from local: $e" }
        }
    }

    companion object {
        private val log = Logger<WorkspaceConfigManager>()
    }
}
