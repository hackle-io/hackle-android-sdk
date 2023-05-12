package io.hackle.android.internal.workspace

import io.hackle.android.HackleConfig
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class PollingWorkspaceHandler(
    private val workspaceCache: WorkspaceCache,
    private val httpWorkspaceFetcher: HttpWorkspaceFetcher,
    private val pollingScheduler: Scheduler,
    private val pollingIntervalMillis: Long,
) : AppStateChangeListener {

    private var pollingJob: ScheduledJob? = null

    fun initialize() {
        poll()
        start()
    }

    private fun poll() {
        try {
            val workspace = httpWorkspaceFetcher.fetch()
            workspaceCache.put(workspace)
            log.debug { "Workspace fetched" }
        } catch (e: Exception) {
            log.error { "Failed to fetch Workspace: $e" }
        }
    }


    private fun start() {
        if (pollingIntervalMillis == HackleConfig.NO_POLLING.toLong()) {
            return
        }
        synchronized(LOCK) {
            if (pollingJob != null) {
                return
            }
            pollingJob = pollingScheduler.schedulePeriodically(
                pollingIntervalMillis,
                pollingIntervalMillis,
                MILLISECONDS,
            ) { poll() }
            log.info { "PollingWorkspaceHandler started polling. Poll every ${pollingIntervalMillis}ms" }
        }
    }

    private fun stop() {
        if (pollingIntervalMillis == HackleConfig.NO_POLLING.toLong()) {
            return
        }
        synchronized(LOCK) {
            pollingJob?.cancel()
            pollingJob = null
            log.info { "PollingWorkspaceHandler stopped polling." }
        }
    }

    override fun onChanged(state: AppState, timestamp: Long) {
        return when (state) {
            FOREGROUND -> start()
            BACKGROUND -> stop()
        }
    }

    companion object {
        private val log = Logger<PollingWorkspaceHandler>()
        private val LOCK = Any()
    }
}
