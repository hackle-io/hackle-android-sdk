package io.hackle.android.internal.event

import io.hackle.android.internal.application.lifecycle.ApplicationState
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.database.repository.EventRepository
import io.hackle.android.internal.database.workspace.EventEntity
import io.hackle.android.internal.database.workspace.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.workspace.EventEntity.Status.PENDING
import io.hackle.android.internal.event.dedup.DedupUserEventFilter
import io.hackle.android.internal.event.dedup.UserEventDedupDeterminer
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.screen.ScreenUserEventDecorator
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.session.SessionUserEventDecorator
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.Screen
import io.hackle.sdk.common.User
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS

class DefaultEventProcessorTest {

    @RelaxedMockK
    private lateinit var eventDedupDeterminer: UserEventDedupDeterminer

    @RelaxedMockK
    private lateinit var eventPublisher: UserEventPublisher

    @RelaxedMockK
    private lateinit var eventExecutor: Executor

    @RelaxedMockK
    private lateinit var eventRepository: EventRepository

    @RelaxedMockK
    private lateinit var eventFlushScheduler: Scheduler

    @RelaxedMockK
    private lateinit var eventDispatcher: EventDispatcher

    @RelaxedMockK
    private lateinit var sessionManager: SessionManager

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var applicationLifecycleManager: ApplicationLifecycleManager

    @RelaxedMockK
    private lateinit var screenManager: ScreenManager

    @RelaxedMockK
    private lateinit var eventBackoffController: UserEventBackoffController

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { eventExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
        every { eventDedupDeterminer.isDedupTarget(any()) } returns false
        every { sessionManager.currentSession } returns null
        every { applicationLifecycleManager.currentState } returns ApplicationState.FOREGROUND
        every { userManager.currentUser } returns User.of("id")
        every { screenManager.currentScreen } returns null
    }


    private fun processor(
        eventPublisher: UserEventPublisher = this.eventPublisher,
        eventExecutor: Executor = this.eventExecutor,
        eventRepository: EventRepository = this.eventRepository,
        eventRepositoryMaxSize: Int = 100,
        eventFlushScheduler: Scheduler = this.eventFlushScheduler,
        eventFlushIntervalMillis: Long = 10_000,
        eventFlushThreshold: Int = 10,
        eventFlushMaxBatchSize: Int = 21,
        eventDispatcher: EventDispatcher = this.eventDispatcher,
        sessionManager: SessionManager = this.sessionManager,
        userManager: UserManager = this.userManager,
        applicationLifecycleManager: ApplicationLifecycleManager = this.applicationLifecycleManager,
        screenManager: ScreenManager = this.screenManager,
        eventBackoffController: UserEventBackoffController = this.eventBackoffController,
    ): DefaultEventProcessor {
        return DefaultEventProcessor(
            eventPublisher = eventPublisher,
            eventExecutor = eventExecutor,
            eventRepository = eventRepository,
            eventRepositoryMaxSize = eventRepositoryMaxSize,
            eventFlushScheduler = eventFlushScheduler,
            eventFlushIntervalMillis = eventFlushIntervalMillis,
            eventFlushThreshold = eventFlushThreshold,
            eventFlushMaxBatchSize = eventFlushMaxBatchSize,
            eventDispatcher = eventDispatcher,
            sessionManager = sessionManager,
            userManager = userManager,
            applicationLifecycleManager = applicationLifecycleManager,
            screenUserEventDecorator = ScreenUserEventDecorator(screenManager),
            eventBackoffController = eventBackoffController
        )
    }

    @Test
    fun `process - eventExecutor 로 실행시킨다`() {
        // given
        val sut = processor()
        every { eventExecutor.execute(any()) } returns Unit
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) {
            eventExecutor.execute(any())
        }
    }

    @Test
    fun `process - fail to execute`() {
        val sut = processor()
        every { eventExecutor.execute(any()) } throws RejectedExecutionException()
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) {
            eventExecutor.execute(any())
        }
    }

    @Test
    fun `process - SessionEvent 인 경우 last event time 을 업데이트 하지 않는다`() {
        // given
        val sut = processor()
        val user = HackleUser.builder().identifier(IdentifierType.ID, "id").build()
        val event = mockk<UserEvent.Track> {
            every { event } returns Event.of("\$session_start")
            every { this@mockk.user } returns user
        }

        // when
        sut.process(event)

        // then
        verify(exactly = 0) { sessionManager.updateLastEventTime(any()) }
    }


    @Test
    fun `process - PushTokenEvent 인 경우 last event time 을 업데이트 하지 않는다`() {
        // given
        val sut = processor()
        val user = HackleUser.builder().identifier(IdentifierType.ID, "id").build()
        val event = UserEvents.track("\$push_token", user = user)

        // when
        sut.process(event)

        // then
        verify(exactly = 0) { sessionManager.updateLastEventTime(any()) }
    }


    @Test
    fun `process - last event time update`() {
        // given
        val sut = processor()
        val user = HackleUser.of("id")
        val event = event(user = user, timestamp = 42)

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { sessionManager.updateLastEventTime(42) }
    }

    @Test
    fun `process - FOREGOURND가 아닌경우 세션초기화 시도`() {
        // given
        val sut = processor()
        val user = HackleUser.of("id")
        val event = event(user = user, timestamp = 42)
        every { applicationLifecycleManager.currentState } returns ApplicationState.BACKGROUND

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { sessionManager.startNewSessionIfNeeded(any(), 42) }
    }

    @Test
    fun `process - 중복제거 대상이면 이벤트를 추가하지 않는다`() {
        // given
        val sut = processor()
        sut.addFilter(DedupUserEventFilter(eventDedupDeterminer))
        every { eventDedupDeterminer.isDedupTarget(any()) } returns true
        val event = event()

        // when
        sut.process(event)

        // then
        verify { eventRepository wasNot Called }
    }

    @Test
    fun `process - currentSession 이 없으면 sessionId 를 추가하지 않는다`() {
        // given
        val sut = processor()
        var savedEvent: UserEvent? = null
        every { eventRepository.save(any()) } answers { savedEvent = firstArg() }
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventRepository.save(any()) }
        expectThat(savedEvent) isSameInstanceAs event
    }

    @Test
    fun `process - currentSession 의 sessionId 를 추가한다`() {
        // given
        val sut = processor()
        sut.addDecorator(SessionUserEventDecorator(sessionManager))
        var savedEvent: UserEvent? = null
        every { eventRepository.save(any()) } answers { savedEvent = firstArg() }
        every { sessionManager.currentSession } returns Session("42.session")
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventRepository.save(any()) }
        expectThat(savedEvent).isNotNull()
            .get { user }.and {
                get { identifiers }.hasSize(2)
                get { identifiers[IdentifierType.SESSION.key] } isEqualTo "42.session"
            }
    }

    @Test
    fun `process - decorate screen`() {
        // given
        val sut = processor()
        var savedEvent: UserEvent? = null
        every { eventRepository.save(any()) } answers { savedEvent = firstArg() }
        every { screenManager.currentScreen } returns Screen("ScreenName", "TestActivity")
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventRepository.save(any()) }
        expectThat(savedEvent).isNotNull().and {
            get { user.hackleProperties["screenName"] } isEqualTo "ScreenName"
            get { user.hackleProperties["screenClass"] } isEqualTo "TestActivity"
        }
    }

    @Test
    fun `process - 입력받은 이벤트를 저장한다`() {
        // given
        val sut = processor()
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventRepository.save(event) }
    }

    @Test
    fun `process - 이벤트 저장 후 저장된 이벤트의 갯수가 최대 저장 갯수보다 큰경우 오래된 이벤트를 삭제`() {
        // given
        val sut = processor(
            eventRepositoryMaxSize = 100,
            eventFlushThreshold = 42,
            eventFlushMaxBatchSize = 51
        )
        every { eventRepository.count(null) } returns 101
        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventRepository.deleteOldEvents(51) }
    }

    @Test
    fun `process - 이벤트 저장 후 flush 해야 되는 경우`() {
        // given
        val sut = processor(
            eventRepositoryMaxSize = 100,
            eventFlushThreshold = 15,
            eventFlushMaxBatchSize = 42
        )
        every { eventRepository.count(null) } returns 100
        every { eventRepository.count(PENDING) } returns 15 // threshold 만큼 이벤트가 있는경우
        every { eventBackoffController.isAllowNextFlush() } returns true

        val events = listOf<EventEntity>(mockk())
        every { eventRepository.getEventsToFlush(42) } returns events

        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventDispatcher.dispatch(events) }
    }

    @Test
    fun `process - 이벤트 저장 후 flush 해야 되는 경우 2`() {
        // given
        val sut = processor(
            eventRepositoryMaxSize = 100,
            eventFlushThreshold = 15,
            eventFlushMaxBatchSize = 42
        )
        every { eventRepository.count(null) } returns 100
        every { eventRepository.count(PENDING) } returns 30 // threshold 의 배수만큼 이벤트가 있는경우
        every { eventBackoffController.isAllowNextFlush() } returns true

        val events = listOf<EventEntity>(mockk())
        every { eventRepository.getEventsToFlush(42) } returns events

        val event = event()

        // when
        sut.process(event)

        // then
        verify(exactly = 1) { eventDispatcher.dispatch(events) }
    }

    @Test
    fun `process - threshold 보다 이벤트가 많지만 threshold 의 배수가 아닌경우 flush 하지 않는다`() {
        // given
        val sut = processor(
            eventRepositoryMaxSize = 100,
            eventFlushThreshold = 15,
            eventFlushMaxBatchSize = 42
        )
        every { eventRepository.count(null) } returns 100
        every { eventRepository.count(PENDING) } returns 29

        val event = event()

        // when
        sut.process(event)

        // then
        verify { eventDispatcher wasNot Called }
    }

    @Test
    fun `process - 이벤트를 publish 한다`() {
        val sut = processor()

        val event = event()
        sut.process(event)


        verify(exactly = 1) { eventPublisher.publish(any()) }
    }

    @Test
    fun `onChanged - FOREGOUND 로 상태가 바뀌면 start() 호출`() {
        // given
        val sut = spyk(processor())

        // when
        sut.onForeground(System.currentTimeMillis(), true)

        // then
        verify(exactly = 1) { sut.start() }
    }

    @Test
    fun `onChanged - BACKGROUND 로 상태가 바뀌면 stop() 호출`() {
        // given
        val sut = spyk(processor())

        // when
        sut.onBackground(System.currentTimeMillis())

        // then
        verify(exactly = 1) { sut.stop() }
    }

    @Test
    fun `initialize - FLUSHING 상태의 이벤트를 PENDING 상태로 바꾼다`() {
        // given
        val sut = processor()
        val events = listOf<EventEntity>(mockk(), mockk())
        every { eventRepository.findAllBy(FLUSHING) } returns events

        // when
        sut.initialize()

        // then
        verify(exactly = 1) { eventRepository.update(events, PENDING) }
    }

    @Test
    fun `initialize - FLUSING 상태의 이벤트가 없으면 별도 처리를 하지 않는다`() {
        // given
        val sut = processor()
        val events = emptyList<EventEntity>()
        every { eventRepository.findAllBy(FLUSHING) } returns events

        // when
        sut.initialize()

        // then
        verify(exactly = 0) { eventRepository.update(any(), any()) }
    }

    @Test
    fun `start - 일정주기로 flush 를 시작한다`() {
        // given
        val sut = processor(eventFlushIntervalMillis = 42)

        // when
        sut.start()

        // then
        val flush = slot<() -> Unit>()
        verify(exactly = 1) {
            eventFlushScheduler.schedulePeriodically(42, 42, MILLISECONDS, capture(flush))
        }

        flush.captured()
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<DefaultEventProcessor.FlushTask>() }) }
    }

    @Test
    fun `start - 이미 시작했으면 또 실행하지 않는다`() {
        val sut = processor(eventFlushIntervalMillis = 42)
        sut.start()
        sut.start()
        verify(exactly = 1) {
            eventFlushScheduler.schedulePeriodically(42, 42, MILLISECONDS, any())
        }
    }

    @Test
    fun `stop`() {

        val sut = processor(eventFlushIntervalMillis = 42)

        sut.start()
        verify(exactly = 1) {
            eventFlushScheduler.schedulePeriodically(
                42,
                42,
                MILLISECONDS,
                any()
            )
        }

        sut.stop()
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<DefaultEventProcessor.FlushTask>() }) }

        sut.start()
        verify(exactly = 2) {
            eventFlushScheduler.schedulePeriodically(
                42,
                42,
                MILLISECONDS,
                any()
            )
        }
    }

    @Test
    fun `close`() {
        val sut = spyk(processor())
        sut.close()

        verify(exactly = 1) { sut.close() }
    }

    @Test
    fun `dispatch - limit 가 0보다 작으면 실행하지 않는다`() {
        // given
        val sut = processor(eventFlushMaxBatchSize = 0).FlushTask()

        // when
        sut.run()

        // then
        verify { eventRepository wasNot Called }
        verify { eventDispatcher wasNot Called }
    }

    @Test
    fun `dispatch - 전송할 이벤트가 없으면 전송하지 않는다`() {
        // given
        val sut = processor(eventRepositoryMaxSize = 1).FlushTask()
        every { eventRepository.getEventsToFlush(any()) } returns emptyList()

        // when
        sut.run()

        // then
        verify { eventDispatcher wasNot Called }
    }

    @Test
    fun `dispatch - 이벤트를 전송한다`() {
        // given
        val sut = processor(eventRepositoryMaxSize = 1).FlushTask()
        val events = listOf<EventEntity>(mockk())
        every { eventRepository.getEventsToFlush(any()) } returns events
        every { eventBackoffController.isAllowNextFlush() } returns true

        // when
        sut.run()

        // then
        verify(exactly = 1) { eventDispatcher.dispatch(events) }
    }

    @Test
    fun `AddEventTask - 예외가 발생해도 무시한다`() {
        val sut = processor().AddEventTask(mockk())
        every { eventRepository.save(any()) } throws IllegalArgumentException()

        try {
            sut.run()
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `FlushTask - 예외가 발생해도 무시한다`() {
        val sut = processor().FlushTask()
        every { eventRepository.getEventsToFlush(any()) } throws IllegalArgumentException()

        try {
            sut.run()
        } catch (e: Exception) {
            fail()
        }
    }


    private fun event(
        timestamp: Long = System.currentTimeMillis(),
        user: HackleUser = HackleUser.of("test_user_id"),
    ): UserEvent {
        val event = mockk<UserEvent> {
            every { this@mockk.timestamp } returns timestamp
            every { this@mockk.user } returns user
        }

        every { event.with(any()) } answers {

            val u = firstArg<HackleUser>()
            return@answers mockk {
                every { this@mockk.timestamp } returns timestamp
                every { this@mockk.user } returns u
            }
        }

        return event
    }
}
