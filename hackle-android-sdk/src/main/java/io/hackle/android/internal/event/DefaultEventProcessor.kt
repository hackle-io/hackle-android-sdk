package io.hackle.android.internal.event

import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.sdk.core.event.EventProcessor
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.internal.utils.tryClose
import java.io.Closeable
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DefaultEventProcessor(
    private val queue: BlockingQueue<UserEvent>,
    private val flushScheduler: Scheduler,
    private val flushIntervalMillis: Long,
    private val eventDispatcher: EventDispatcher,
    private val maxEventDispatchSize: Int,
    private val deduplicationDeterminer: ExposureEventDeduplicationDeterminer,
) : EventProcessor, AppStateChangeListener, Closeable {

    private var flushingJob: ScheduledJob? = null

    override fun process(event: UserEvent) {

        if (deduplicationDeterminer.isDeduplicationTarget(event)) {
            return
        }

        if (!queue.offer(event)) {
            log.warn { "Event not processed. Exceeded event queue capacity" }
            return
        }
        if (queue.flushNeeded) {
            flush()
        }
    }

    private fun flush() {
        queue.flush()
    }

    override fun onChanged(state: AppState) {
        when (state) {
            FOREGROUND -> start()
            BACKGROUND -> stop()
        }.safe
    }

    fun start() {
        synchronized(LOCK) {
            if (flushingJob != null) {
                return
            }

            flushingJob = flushScheduler.schedulePeriodically(flushIntervalMillis, flushIntervalMillis, MILLISECONDS) { flush() }
            log.info { "DefaultEventProcessor started. Flush events every $flushIntervalMillis ms" }
        }
    }

    fun stop() {
        synchronized(LOCK) {
            flushingJob?.cancel()
            flushingJob = null
            flush()
            log.info { "DefaultEventProcessor stopped." }
        }
    }

    override fun close() {
        stop()
        flushScheduler.tryClose()
        flush()
        eventDispatcher.tryClose()
    }

    private fun BlockingQueue<UserEvent>.flush() {
        val pendingEvents = mutableListOf<UserEvent>().apply { drainTo(this) }
        if (pendingEvents.isNotEmpty()) {
            eventDispatcher.dispatch(pendingEvents)
        }
    }

    private val BlockingQueue<UserEvent>.flushNeeded: Boolean get() = size >= maxEventDispatchSize

    companion object {
        private val log = Logger<DefaultEventProcessor>()
        private val LOCK = Any()
    }
}