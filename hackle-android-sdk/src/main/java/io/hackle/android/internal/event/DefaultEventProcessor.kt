package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.android.internal.database.EventRepository
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DefaultEventProcessor(
    private val eventStorageMaxSize: Long,
    private val eventRepository: EventRepository,
    private val eventExecutor: ExecutorService,
    private val flushScheduler: Scheduler,
    private val flushIntervalMillis: Long,
    private val eventDispatcher: EventDispatcher,
    private val eventDispatchThreshold: Long,
    private val eventDispatchMaxSize: Long,
    private val deduplicationDeterminer: ExposureEventDeduplicationDeterminer,
) : EventProcessor, AppStateChangeListener, Closeable {

    private var flushingJob: ScheduledJob? = null

    override fun process(event: UserEvent) {
        if (deduplicationDeterminer.isDeduplicationTarget(event)) {
            return
        }
        addEvent(event)
    }

    private fun addEvent(event: UserEvent) {
        try {
            eventExecutor.submit(AddEventTask(event))
        } catch (e: Exception) {
            log.error { "Failed to submit AddEventTask: $e" }
        }
    }

    private fun flush() {
        try {
            eventExecutor.submit(FlushTask())
        } catch (e: Exception) {
            log.error { "Failed to submit FlushTask: $e" }
        }
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

            flushingJob = flushScheduler.schedulePeriodically(
                flushIntervalMillis, flushIntervalMillis, MILLISECONDS) { flush() }
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
        flushScheduler.tryClose()
        stop()
    }

    private fun dispatch(limit: Long) {
        if (limit <= 0) {
            return
        }

        val events = eventRepository.getEventsToFlush(limit)
        if (events.isEmpty()) {
            return
        }
        eventDispatcher.dispatch(events)
    }

    inner class AddEventTask(private val event: UserEvent) : Runnable {
        override fun run() {
            try {
                eventRepository.save(event)

                val totalCount = eventRepository.count()
                if (totalCount > eventStorageMaxSize) {
                    eventRepository.delete(eventDispatchThreshold)
                }

                val pendingCount = eventRepository.count(PENDING)
                if (pendingCount >= eventDispatchThreshold && pendingCount % eventDispatchThreshold == 0L) {
                    dispatch(eventDispatchMaxSize)
                }
            } catch (e: Exception) {
                log.error { "Failed to add event: $e" }
            }
        }
    }

    inner class FlushTask : Runnable {
        override fun run() {
            try {
                dispatch(eventDispatchMaxSize)
            } catch (e: Exception) {
                log.error { "Failed to flush events: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<DefaultEventProcessor>()
        private val LOCK = Any()
    }
}
