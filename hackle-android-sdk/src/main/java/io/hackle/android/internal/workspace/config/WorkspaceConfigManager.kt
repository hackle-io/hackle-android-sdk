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

    private val record: AtomicReference<WorkspaceConfigRecord?> = AtomicReference()

    override fun initialize() {
        load()
    }

    override fun metadata(): Workspace.Metadata? {
        return record.get()?.workspace()?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceConfig? {
        return record.get()?.workspace()
    }

    override fun sync(): CompletableFuture<Void> {
        val lastModified = record.get()?.lastModified
        return fetcher.fetchIfModified(lastModified)
            .consume { store(it) }
            .recover { log.error { "Failed to fetch WorkspaceConfig: $it" } }
    }

    private fun store(record: WorkspaceConfigRecord?) {
        if (record == null) {
            return
        }
        this.record.set(record)
        this.repository.set(record)
    }

    private fun load() {
        try {
            val record = repository.get()
            if (record != null) {
                this.record.set(record)
                log.debug { "WorkspaceConfig loaded: [last modified: ${record.lastModified}]" }
            }
        } catch (e: Exception) {
            log.error { "Failed to read WorkspaceConfig from local: $e" }
        }
    }

    companion object {
        private val log = Logger<WorkspaceConfigManager>()
    }
}
