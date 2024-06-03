package io.hackle.android.internal.sync

import io.hackle.android.HackleConfig
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class PollingSynchronizer(
    private val delegate: CompositeSynchronizer,
    private val scheduler: Scheduler,
    private val intervalMillis: Long,
) : Synchronizer, AppStateListener {

    private var pollingJob: ScheduledJob? = null

    override fun sync() {
        try {
            delegate.sync()
        } catch (e: Exception) {
            log.error { "Failed to sync $delegate: $e" }
        }
    }

    fun sync(type: SynchronizerType) {
        try {
            delegate.sync(type)
        } catch (e: Exception) {
            log.error { "Failed to sync $delegate: $e" }
        }
    }

    fun start() {
        if (intervalMillis == HackleConfig.NO_POLLING.toLong()) {
            return
        }

        synchronized(LOCK) {
            if (pollingJob != null) {
                return
            }

            pollingJob = scheduler.schedulePeriodically(
                intervalMillis,
                intervalMillis,
                MILLISECONDS
            ) { sync() }
            log.info { "$this started polling. Poll every ${intervalMillis}ms." }
        }
    }

    fun stop() {
        if (intervalMillis == HackleConfig.NO_POLLING.toLong()) {
            return
        }
        synchronized(LOCK) {
            pollingJob?.cancel()
            pollingJob = null
            log.info { "$this stopped polling." }
        }
    }

    override fun onState(state: AppState, timestamp: Long) {
        return when (state) {
            AppState.FOREGROUND -> start()
            AppState.BACKGROUND -> stop()
        }
    }

    override fun toString(): String {
        return "PollingSynchronizer($delegate, ${intervalMillis}ms)"
    }

    companion object {
        private val LOCK = Any()
        private val log = Logger<PollingSynchronizer>()
    }
}
