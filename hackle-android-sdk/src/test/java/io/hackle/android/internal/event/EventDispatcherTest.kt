package io.hackle.android.internal.event

import io.hackle.android.internal.database.EventEntity
import io.hackle.android.internal.database.EventEntity.Status.FLUSHING
import io.hackle.android.internal.database.EventEntity.Status.PENDING
import io.hackle.android.internal.database.EventEntity.Type.TRACK
import io.hackle.android.internal.database.EventRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

class EventDispatcherTest {

    @RelaxedMockK
    private lateinit var eventExecutor: Executor

    @RelaxedMockK
    private lateinit var eventRepository: EventRepository

    @RelaxedMockK
    private lateinit var httpExecutor: Executor

    @MockK
    private lateinit var httpClient: OkHttpClient

    private lateinit var sut: EventDispatcher

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { eventExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
        every { httpExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
        sut = EventDispatcher(
            "http://localhost",
            eventExecutor,
            eventRepository,
            httpExecutor,
            httpClient)
    }

    @Test
    fun `dispatchExecutor 로 EventDispatchTask 를 실행시킨다`() {
        sut.dispatch(emptyList())
        verify(exactly = 1) {
            httpExecutor.execute(withArg {
                expectThat(it).isA<EventDispatcher.EventDispatchTask>()
            })
        }
    }

    @Test
    fun `EventDispatchTask 실행 요청에 실패하면 다시 PENDING 상태로 바꿔놓는다`() {
        every { httpExecutor.execute(any()) } answers { throw RejectedExecutionException() }

        try {
            sut.dispatch(emptyList())
        } catch (e: Exception) {
            fail()
        }

        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<EventDispatcher.UpdateEventToPendingTask>() }) }
    }

    @Test
    fun `dispatch - 이벤트 전송에 성공하면 해당 이벤트를 DB 에서 지운다`() {
        // given
        mockResponse(202)
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))

        // when
        sut.dispatch(events)

        // then
        verify(exactly = 1) { eventRepository.delete(events) }
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<EventDispatcher.DeleteEventTask>() }) }
    }

    @Test
    fun `dispatch - 이벤트 전송시 4xx 에러가 발생하면 해당 이벤트를 DB 에서 지운다`() {
        // given
        mockResponse(400)
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))

        // when
        sut.dispatch(events)

        // then
        verify(exactly = 1) { eventRepository.delete(events) }
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<EventDispatcher.DeleteEventTask>() }) }
    }

    @Test
    fun `dispatch - 이벤트 전송에 실패하면 재시도를 위해 다시 PENDING 상태로 변경한다`() {
        // given
        mockResponse(500)
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))

        // when
        sut.dispatch(events)

        // then
        verify(exactly = 1) { eventRepository.update(events, PENDING) }
        verify(exactly = 1) { eventExecutor.execute(withArg { expectThat(it).isA<EventDispatcher.UpdateEventToPendingTask>() }) }
    }


    @Test
    fun `DeleteEventTask - 이벤트를 모두 삭제한다`() {
        // then
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))
        val task = sut.DeleteEventTask(events)

        // when
        task.run()

        // then
        verify(exactly = 1) { eventRepository.delete(events) }
    }

    @Test
    fun `DeleteEventTask - 이벤트삭제하다 예외가 발생해도 무시한다`() {
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))
        val task = sut.DeleteEventTask(events)

        every { eventRepository.delete(any<List<EventEntity>>()) } throws IllegalArgumentException()

        try {
            task.run()
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `UpdateEventTask - 이벤트를 PENDING 상태로 업데이트한다`() {
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))
        val task = sut.UpdateEventToPendingTask(events)

        task.run()

        // then
        verify(exactly = 1) { eventRepository.update(events, PENDING) }
    }

    @Test
    fun `UpdateEventTask - 업데이트하다 예외가 발생해도 무시한다`() {
        val events = listOf(EventEntity(1, FLUSHING, TRACK, "body"))
        val task = sut.UpdateEventToPendingTask(events)
        every { eventRepository.update(any(), any()) } throws IllegalArgumentException()

        try {
            task.run()
        } catch (e: Exception) {
            fail()
        }
    }

    private fun mockResponse(code: Int) {
        val response = mockk<Response> {
            every { code() } returns code
        }
        val call = mockk<Call> {
            every { execute() } returns response
        }
        every { httpClient.newCall(any()) } returns call
    }
}
