package io.hackle.android.internal.task

import io.hackle.android.support.assertThrows
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

class CompletableFutureExtensionsTest {

    private val directExecutor = Executor { it.run() }

    private fun <T> failedFuture(exception: Throwable = IllegalArgumentException("fail")): CompletableFuture<T> {
        return CompletableFuture<T>().apply { completeExceptionally(exception) }
    }

    // Futures

    @Test
    fun `async - executor에서 블록을 실행해 결과로 완료한다`() {
        // given
        val executed = mutableListOf<String>()
        val executor = Executor {
            executed.add("executed")
            it.run()
        }

        // when
        val actual = Futures.async(executor) { 42 }

        // then
        expectThat(actual.get()) isEqualTo 42
        expectThat(executed) isEqualTo mutableListOf("executed")
    }

    @Test
    fun `async - 블록이 예외를 던지면 예외로 완료된다`() {
        // when
        val actual = Futures.async<Int>(directExecutor) { throw IllegalArgumentException("fail") }

        // then
        val exception = assertThrows<ExecutionException> { actual.get() }
        expectThat(exception.cause).isA<IllegalArgumentException>()
            .get { message } isEqualTo "fail"
    }

    @Test
    fun `sync - 블록 결과로 완료된 future를 만든다`() {
        expectThat(Futures.sync { 42 }.get()) isEqualTo 42
    }

    @Test
    fun `sync - 블록이 예외를 던지면 던지지 않고 예외로 완료된 future를 만든다`() {
        // when
        val actual = Futures.sync<Int> { throw IllegalArgumentException("fail") }

        // then
        val exception = assertThrows<ExecutionException> { actual.get() }
        expectThat(exception.cause).isA<IllegalArgumentException>()
    }

    @Test
    fun `wrap - 블록이 리턴한 future를 그대로 리턴한다`() {
        // given
        val future = CompletableFuture.completedFuture(42)

        // when
        val actual = Futures.wrap { future }

        // then
        expectThat(actual) isSameInstanceAs future
    }

    @Test
    fun `wrap - 블록이 동기적으로 예외를 던지면 던지지 않고 예외로 완료된 future로 바꾼다`() {
        // when
        val actual = Futures.wrap<Int> { throw IllegalArgumentException("fail") }

        // then
        val exception = assertThrows<ExecutionException> { actual.get() }
        expectThat(exception.cause).isA<IllegalArgumentException>()
    }

    @Test
    fun `allOf - 모든 future가 완료되어야 완료된다`() {
        // given
        val future1 = CompletableFuture<Void>()
        val future2 = CompletableFuture<Void>()

        // when
        val actual = Futures.allOf(listOf(future1, future2))

        // then
        expectThat(actual.isDone) isEqualTo false
        future1.complete(null)
        expectThat(actual.isDone) isEqualTo false
        future2.complete(null)
        actual.get()
    }

    @Test
    fun `allOf - 하나라도 실패하면 예외로 완료된다`() {
        // given
        val future1 = CompletableFuture.completedFuture<Void>(null)
        val future2 = failedFuture<Void>()

        // when
        val actual = Futures.allOf(listOf(future1, future2))

        // then
        val exception = assertThrows<ExecutionException> { actual.get() }
        expectThat(exception.cause).isA<IllegalArgumentException>()
    }

    @Test
    fun `completed - 성공으로 완료된 future를 리턴한다`() {
        expectThat(Futures.completed().get()).isNull()
    }

    @Test
    fun `asFuture - 값으로 완료된 future를 만든다`() {
        expectThat(42.asFuture().get()) isEqualTo 42
    }

    // map

    @Test
    fun `map - 결과를 변환한다`() {
        val actual = 42.asFuture().map { it * 2 }
        expectThat(actual.get()) isEqualTo 84
    }

    @Test
    fun `map - 원본이 실패하면 변환하지 않고 실패를 전파한다`() {
        // given
        val transformed = mutableListOf<Int>()

        // when
        val actual = failedFuture<Int>().map { transformed.add(it) }

        // then
        assertThrows<ExecutionException> { actual.get() }
        expectThat(transformed) isEqualTo mutableListOf<Int>()
    }

    @Test
    fun `mapAsync - executor에서 변환한다`() {
        // given
        val executor = mockk<Executor>()
        val runnable = slot<Runnable>()
        every { executor.execute(capture(runnable)) } answers { }

        // when
        val actual = 42.asFuture().mapAsync(executor) { it * 2 }

        // then
        expectThat(actual.isDone) isEqualTo false
        runnable.captured.run()
        expectThat(actual.get()) isEqualTo 84
    }

    // consume

    @Test
    fun `consume - 결과를 소비하고 완료한다`() {
        // given
        val consumed = mutableListOf<Int>()

        // when
        val actual = 42.asFuture().consume { consumed.add(it) }

        // then
        expectThat(actual.get()).isNull()
        expectThat(consumed) isEqualTo mutableListOf(42)
    }

    @Test
    fun `consumeAsync - executor에서 소비한다`() {
        // given
        val consumed = mutableListOf<Int>()
        val executor = mockk<Executor>()
        val runnable = slot<Runnable>()
        every { executor.execute(capture(runnable)) } answers { }

        // when
        val actual = 42.asFuture().consumeAsync(executor) { consumed.add(it) }

        // then
        expectThat(actual.isDone) isEqualTo false
        expectThat(consumed) isEqualTo mutableListOf<Int>()

        runnable.captured.run()
        actual.get()
        expectThat(consumed) isEqualTo mutableListOf(42)
    }

    // flatMap

    @Test
    fun `flatMap - 결과를 다음 future로 연결한다`() {
        val actual = 42.asFuture().flatMap { (it * 2).asFuture() }
        expectThat(actual.get()) isEqualTo 84
    }

    @Test
    fun `flatMap - 다음 future의 실패를 전파한다`() {
        // when
        val actual = 42.asFuture().flatMap<Int, Int> { failedFuture() }

        // then
        val exception = assertThrows<ExecutionException> { actual.get() }
        expectThat(exception.cause).isA<IllegalArgumentException>()
    }

    @Test
    fun `flatMapAsync - executor에서 연결한다`() {
        // given
        val executor = mockk<Executor>()
        val runnable = slot<Runnable>()
        every { executor.execute(capture(runnable)) } answers { }

        // when
        val actual = 42.asFuture().flatMapAsync(executor) { (it * 2).asFuture() }

        // then
        expectThat(actual.isDone) isEqualTo false
        runnable.captured.run()
        expectThat(actual.get()) isEqualTo 84
    }

    // recover

    @Test
    fun `recover - 실패를 대체값으로 복구한다`() {
        val actual = failedFuture<Int>().recover { 42 }
        expectThat(actual.get()) isEqualTo 42
    }

    @Test
    fun `recover - 성공이면 복구하지 않는다`() {
        // given
        val recovered = mutableListOf<Throwable>()

        // when
        val actual = 42.asFuture().recover {
            recovered.add(it)
            0
        }

        // then
        expectThat(actual.get()) isEqualTo 42
        expectThat(recovered) isEqualTo mutableListOf<Throwable>()
    }

    @Test
    fun `recover(Void) - 실패를 소비하고 성공으로 완료한다`() {
        // given
        val recovered = mutableListOf<Throwable>()

        // when
        val actual = failedFuture<Void>().recover { recovered.add(it) }

        // then
        expectThat(actual.get()).isNull()
        expectThat(recovered.size) isEqualTo 1
    }

    // onSuccess, onFailure, onComplete

    @Test
    fun `onSuccess - 성공했을 때만 실행되고 결과를 유지한다`() {
        // given
        val succeeded = mutableListOf<Int>()

        // when
        val success = 42.asFuture().onSuccess { succeeded.add(it) }
        val failure = failedFuture<Int>().onSuccess { succeeded.add(it) }

        // then
        expectThat(success.get()) isEqualTo 42
        assertThrows<ExecutionException> { failure.get() }
        expectThat(succeeded) isEqualTo mutableListOf(42)
    }

    @Test
    fun `onSuccessAsync - executor에서 실행되고 실행이 끝난 뒤 완료된다`() {
        // given
        val succeeded = mutableListOf<Int>()
        val executor = mockk<Executor>()
        val runnable = slot<Runnable>()
        every { executor.execute(capture(runnable)) } answers { }

        // when
        val actual = 42.asFuture().onSuccessAsync(executor) { succeeded.add(it) }

        // then
        expectThat(actual.isDone) isEqualTo false
        runnable.captured.run()
        expectThat(actual.get()) isEqualTo 42
        expectThat(succeeded) isEqualTo mutableListOf(42)
    }

    @Test
    fun `onFailure - 실패했을 때만 실행되고 실패는 유지된다`() {
        // given
        val failed = mutableListOf<Throwable>()

        // when
        val success = 42.asFuture().onFailure { failed.add(it) }
        val failure = failedFuture<Int>().onFailure { failed.add(it) }

        // then
        expectThat(success.get()) isEqualTo 42
        assertThrows<ExecutionException> { failure.get() }
        expectThat(failed.size) isEqualTo 1
    }

    @Test
    fun `onComplete - 성공과 실패 모두 실행된다`() {
        // given
        val completed = mutableListOf<String>()

        // when
        42.asFuture().onComplete { completed.add("success") }.get()
        val failure = failedFuture<Int>().onComplete { completed.add("failure") }

        // then
        assertThrows<ExecutionException> { failure.get() }
        expectThat(completed) isEqualTo mutableListOf("success", "failure")
    }

    @Test
    fun `onCompleteAsync - executor에서 실행된다`() {
        // given
        val completed = mutableListOf<String>()
        val executor = mockk<Executor>()
        val runnable = slot<Runnable>()
        every { executor.execute(capture(runnable)) } answers { }

        // when
        val actual = 42.asFuture().onCompleteAsync(executor) { completed.add("completed") }

        // then
        expectThat(actual.isDone) isEqualTo false
        runnable.captured.run()
        expectThat(actual.get()) isEqualTo 42
        expectThat(completed) isEqualTo mutableListOf("completed")
    }
}
