package io.hackle.android.internal.event

import io.hackle.android.internal.database.repository.EventRepository
import io.hackle.android.internal.database.workspace.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.workspace.EventEntity.Status.PENDING
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.android.internal.lifecycle.AppStateListener
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.session.SessionEventTracker
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.core.event.EventProcessor
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.time.Clock
import io.hackle.sdk.core.internal.utils.safe
import io.hackle.sdk.core.internal.utils.tryClose
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DefaultEventProcessor(
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
    private val screenUserEventDecorator: UserEventDecorator,
    private val eventBackoffController: UserEventBackoffController,
) : EventProcessor, AppStateListener, Closeable {

    private var flushingJob: ScheduledJob? = null

    private val filters = CopyOnWriteArrayList<UserEventFilter>()
    private val decorators = CopyOnWriteArrayList<UserEventDecorator>()


    fun addFilter(filter: UserEventFilter) {
        filters.add(filter)
        log.debug { "UserEventFilter added [${filter.javaClass.simpleName}]" }
    }

    fun addDecorator(decorator: UserEventDecorator) {
        decorators.add(decorator)
        log.debug { "UserEventDecorator added [${decorator.javaClass.simpleName}]" }
    }

    override fun process(event: UserEvent) {
        try {
            // NOTE: screen decorator는 task에 들어가기 전에 추가해야 한다.
            //  task에 들어간 후 처리되기 전에 screen이 바뀔 수 있기 때문
            val decoratedEvent = screenUserEventDecorator.decorate(event)
            eventExecutor.execute(AddEventTask(decoratedEvent))
        } catch (e: Exception) {
            log.error { "Failed to process event: $e" }
        }
    }

    override fun flush() {
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

            val expirationThresholdTimestamp =
                Clock.SYSTEM.currentMillis() - Constants.USER_EVENT_EXPIRED_INTERVAL_MILLIS
            eventRepository.deleteExpiredEvents(expirationThresholdTimestamp)
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

    override fun onState(state: AppState, timestamp: Long) {
        when (state) {
            FOREGROUND -> start()
            BACKGROUND -> stop()
        }.safe
    }

    override fun close() {
        eventFlushScheduler.tryClose()
        stop()
    }

    private fun flushInternal() {
        if (!eventBackoffController.isAllowNextFlush()) {
            return
        }
        dispatch(eventFlushMaxBatchSize)
    }

    inner class AddEventTask(private val event: UserEvent) : Runnable {
        override fun run() {
            try {
                update(event)
                if (filters.any { it.check(event).isBlock }) {
                    return
                }

                val decoratedEvent =
                    decorators.fold(event) { userEvent, decorator -> decorator.decorate(userEvent) }

                save(decoratedEvent)
                eventPublisher.publish(decoratedEvent)
            } catch (e: Exception) {
                log.error { "Failed to add event: $e" }
            }
        }

        private fun update(event: UserEvent) {
            if (SessionEventTracker.isSessionEvent(event) || PushEventTracker.isPushTokenEvent(event)) {
                return
            }

            if (appStateManager.currentState == FOREGROUND) {
                sessionManager.updateLastEventTime(event.timestamp)
            } else {
                // Corner case when an event is processed between onPause and onResume
                sessionManager.startNewSessionIfNeeded(userManager.currentUser, event.timestamp)
            }
        }

        private fun save(event: UserEvent) {
            eventRepository.save(event)


            val totalCount = eventRepository.count()
            if (totalCount > eventRepositoryMaxSize) {
                eventRepository.deleteOldEvents(eventFlushMaxBatchSize)
            }

            val pendingCount = eventRepository.count(PENDING)
            if (pendingCount >= eventFlushThreshold && pendingCount % eventFlushThreshold == 0L) {
                flushInternal()
            }
        }
    }

    inner class FlushTask : Runnable {
        override fun run() {
            try {
                flushInternal()
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
