package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.android.internal.database.EventRepository
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateChangeListener
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.session.SessionEventTracker
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.core.event.EventProcessor
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.internal.utils.tryClose
import io.hackle.sdk.core.user.IdentifierType
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DefaultEventProcessor(
    private val deduplicationDeterminer: ExposureEventDeduplicationDeterminer,
    private val eventPublisher: UserEventPublisher,
    private val eventExecutor: Executor,
    private val eventRepository: EventRepository,
    private val eventRepositoryMaxSize: Int,
    private val eventFlushScheduler: Scheduler,
    private val eventFlushIntervalMillis: Long,
    private val eventFlushThreshold: Int,
    private val eventFlushMaxBatchSize: Int,
    private val eventDispatcher: EventDispatcher,
    private val sessionManager: SessionManager,
    private val userManager: UserManager,
    private val appStateManager: AppStateManager,
    private val activityProvider: ActivityProvider,
) : EventProcessor, AppStateChangeListener, Closeable {

    private var flushingJob: ScheduledJob? = null

    override fun process(event: UserEvent) {
        try {
            val newEvent = decorateScreenName(event)
            eventExecutor.execute(AddEventTask(newEvent))
        } catch (e: Exception) {
            log.error { "Failed to process event: $e" }
        }
    }

    private fun flush() {
        try {
            eventExecutor.execute(FlushTask())
        } catch (e: Exception) {
            log.error { "Failed to submit FlushTask: $e" }
        }
    }

    fun initialize() {
        try {
            start()
            val events = eventRepository.findAllBy(FLUSHING)
            if (events.isNotEmpty()) {
                eventRepository.update(events, PENDING)
            }
            log.debug { "DefaultEventProcessor initialized." }
        } catch (e: Exception) {
            log.error { "Fail to initialize: $e" }
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

    fun dispatch(limit: Int) {
        if (limit <= 0) {
            return
        }

        val events = eventRepository.getEventsToFlush(limit)
        if (events.isEmpty()) {
            return
        }
        eventDispatcher.dispatch(events)
    }

    override fun onChanged(state: AppState, timestamp: Long) {
        when (state) {
            FOREGROUND -> start()
            BACKGROUND -> stop()
        }.safe
    }

    override fun close() {
        eventFlushScheduler.tryClose()
        stop()
    }

    inner class AddEventTask(private val event: UserEvent) : Runnable {
        override fun run() {
            try {
                update(event)
                if (deduplicationDeterminer.isDeduplicationTarget(event)) {
                    return
                }

                val newEvent = decorateSession(event)

                save(newEvent)
                eventPublisher.publish(newEvent)
            } catch (e: Exception) {
                log.error { "Failed to add event: $e" }
            }
        }

        private fun update(event: UserEvent) {
            if (SessionEventTracker.isSessionEvent(event)) {
                return
            }

            if (appStateManager.currentState == FOREGROUND) {
                sessionManager.updateLastEventTime(event.timestamp)
            } else {
                // Corner case when an event is processed between onPause and onResume
                sessionManager.startNewSessionIfNeeded(userManager.currentUser, event.timestamp)
            }
        }

        private fun decorateSession(event: UserEvent): UserEvent {
            if (event.user.sessionId != null) {
                return event
            }

            val session = sessionManager.currentSession ?: return event

            val newUser = event.user.toBuilder()
                .identifier(IdentifierType.SESSION, session.id, overwrite = false)
                .build()
            return event.with(newUser)
        }


        private fun save(event: UserEvent) {
            eventRepository.save(event)


            val totalCount = eventRepository.count()
            if (totalCount > eventRepositoryMaxSize) {
                eventRepository.deleteOldEvents(eventFlushMaxBatchSize)
            }

            val pendingCount = eventRepository.count(PENDING)
            if (pendingCount >= eventFlushThreshold && pendingCount % eventFlushThreshold == 0L) {
                dispatch(eventFlushMaxBatchSize)
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

    private fun decorateScreenName(event: UserEvent): UserEvent {
        val currentActivity = activityProvider.currentActivity ?: return event

        val newUser = event.user.toBuilder()
            .hackleProperty("screenClass", currentActivity.javaClass.simpleName)
            .build()

        return event.with(newUser)
    }

    companion object {
        private val log = Logger<DefaultEventProcessor>()
        private val LOCK = Any()
    }
}
