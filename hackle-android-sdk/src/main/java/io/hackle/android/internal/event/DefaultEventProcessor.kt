package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
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
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DefaultEventProcessor(
    private val deduplicationDeterminer: ExposureEventDeduplicationDeterminer,
    private val eventExecutor: Executor,
    private val eventRepository: EventRepository,
    private val eventRepositoryMaxSize: Int,
    private val eventFlushScheduler: Scheduler,
    private val eventFlushIntervalMillis: Long,
    private val eventFlushThreshold: Int,
    private val eventFlushMaxBatchSize: Int,
    private val eventDispatcher: EventDispatcher,
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
            eventExecutor.execute(AddEventTask(event))
        } catch (e: Exception) {
            log.error { "Failed to submit AddEventTask: $e" }
        }
    }

    private fun flush() {
        try {
            eventExecutor.execute(FlushTask())
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

    fun initialize() {
        try {
            eventExecutor.execute(InitializeTask())
        } catch (e: Exception) {
            log.error { "Failed to submit InitializeTask: $e" }
        }
    }

    fun start() {
        synchronized(LOCK) {
            if (flushingJob != null) {
                return
            }

            flushingJob = eventFlushScheduler.schedulePeriodically(
                eventFlushIntervalMillis,
                eventFlushIntervalMillis,
                MILLISECONDS
            ) { flush() }
            log.info { "DefaultEventProcessor started. Flush events every $eventFlushIntervalMillis ms" }
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
        eventFlushScheduler.tryClose()
        stop()
    }

    private fun dispatch(limit: Int) {
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
                if (totalCount > eventRepositoryMaxSize) {
                    eventRepository.deleteOldEvents(eventFlushMaxBatchSize)
                }

                val pendingCount = eventRepository.count(PENDING)
                if (pendingCount >= eventFlushThreshold && pendingCount % eventFlushThreshold == 0L) {
                    dispatch(eventFlushMaxBatchSize)
                }
            } catch (e: Exception) {
                log.error { "Failed to add event: $e" }
            }
        }
    }

    inner class FlushTask : Runnable {
        override fun run() {
            try {
                dispatch(eventFlushMaxBatchSize)
            } catch (e: Exception) {
                log.error { "Failed to flush events: $e" }
            }
        }
    }

    inner class InitializeTask : Runnable {
        override fun run() {
            try {
                val events = eventRepository.findAllBy(FLUSHING)
                if (events.isNotEmpty()) {
                    eventRepository.update(events, PENDING)
                }
            } catch (e: Exception) {
                log.error { "Fail to initialize: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<DefaultEventProcessor>()
        private val LOCK = Any()
    }
}
