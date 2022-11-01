package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity
import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.android.internal.database.EventRepository
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.MILLISECONDS

class DefaultEventProcessorTest {

    @RelaxedMockK
    private lateinit var deduplicationDeterminer: ExposureEventDeduplicationDeterminer

    @RelaxedMockK
    private lateinit var eventExecutor: Executor

    @RelaxedMockK
    private lateinit var eventRepository: EventRepository

    @RelaxedMockK
    private lateinit var eventFlushScheduler: Scheduler

    @RelaxedMockK
    private lateinit var eventDispatcher: EventDispatcher

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { eventExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
        every { deduplicationDeterminer.isDeduplicationTarget(any()) } returns false
    }


    private fun processor(
        deduplicationDeterminer: ExposureEventDeduplicationDeterminer = this.deduplicationDeterminer,
        eventExecutor: Executor = this.eventExecutor,
        eventRepository: EventRepository = this.eventRepository,
        eventRepositoryMaxSize: Int = 100,
        eventFlushScheduler: Scheduler = this.eventFlushScheduler,
        eventFlushIntervalMillis: Long = 10_000,
        eventFlushThreshold: Int = 10,
        eventFlushMaxBatchSize: Int = 21,
        eventDispatcher: EventDispatcher = this.eventDispatcher,
    ): DefaultEventProcessor {
        return DefaultEventProcessor(
            deduplicationDeterminer,
            eventExecutor,
            eventRepository,
            eventRepositoryMaxSize,
            eventFlushScheduler,
            eventFlushIntervalMillis,
            eventFlushThreshold,
            eventFlushMaxBatchSize,
            eventDispatcher
        )
    }

    @Test
    fun `process - 중복제거 대상이면 이벤트를 추가하지 않는다`() {
        // given
        val sut = processor()
        every { deduplicationDeterminer.isDeduplicationTarget(any()) } returns true
        val event = mockk<UserEvent>()

        // when
        sut.process(event)

        // then
        verify { eventExecutor wasNot Called }
        verify { eventRepository wasNot Called }
    }

    @Test
    fun `process - 입력받은 이벤트를 저장한다`() {
        // given
        val sut = processor()
        val event = mockk<UserEvent>()

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
        val event = mockk<UserEvent>()

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

        val events = listOf<EventEntity>(mockk())
        every { eventRepository.getEventsToFlush(42) } returns events

        val event = mockk<UserEvent>()

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

        val events = listOf<EventEntity>(mockk())
        every { eventRepository.getEventsToFlush(42) } returns events

        val event = mockk<UserEvent>()

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

        val event = mockk<UserEvent>()

        // when
        sut.process(event)

        // then
        verify { eventDispatcher wasNot Called }
    }

    @Test
    fun `onChanged - FOREGOUND 로 상태가 바뀌면 start() 호출`() {
        // given
        val sut = spyk(processor())

        // when
        sut.onChanged(AppState.FOREGROUND)

        // then
        verify(exactly = 1) { sut.start() }
    }

    @Test
    fun `onChanged - BACKGROUND 로 상태가 바뀌면 stop() 호출`() {
        // given
        val sut = spyk(processor())

        // when
        sut.onChanged(AppState.BACKGROUND)

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
            eventFlushScheduler.schedulePeriodically(42,
                42,
                MILLISECONDS,
                any())
        }

        sut.stop()
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<DefaultEventProcessor.FlushTask>() }) }

        sut.start()
        verify(exactly = 2) {
            eventFlushScheduler.schedulePeriodically(42,
                42,
                MILLISECONDS,
                any())
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

    @Test
    fun `InitializeTask - 예외가 발생해도 무시한다`() {
        val sut = processor().InitializeTask()
        every { eventRepository.findAllBy(any()) } throws IllegalArgumentException()

        try {
            sut.run()
        } catch (e: Exception) {
            fail()
        }
    }
}