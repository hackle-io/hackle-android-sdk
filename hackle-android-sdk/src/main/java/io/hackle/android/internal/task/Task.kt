package io.hackle.android.internal.task

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

internal class Task<T> {

    private val lock = Any()
    private var result: Result<T>? = null
    private var listeners = ArrayList<(Result<T>) -> Unit>(2)

    // --- 생성/완료 (producer, 내부 모듈 전용 권장) -------------------------

    fun success(value: T): Boolean = settle(Result.success(value))

    fun failure(e: Throwable): Boolean = settle(Result.failure(e))

    val isCompleted: Boolean get() = synchronized(lock) { result != null }

    private fun settle(r: Result<T>): Boolean {
        val toNotify: List<(Result<T>) -> Unit>
        synchronized(lock) {
            if (result != null) return false // 최초 1회만 완료 허용
            result = r
            toNotify = ArrayList(listeners)
            listeners.clear()
        }
        for (listener in toNotify) {
            listener(r)
        }
        return true
    }

    /** 모든 연산자가 의존하는 내부 구독 프리미티브. 이미 완료 시 현재 스레드에서 즉시 실행. */
    private fun onResult(listener: (Result<T>) -> Unit) {
        val r: Result<T>
        synchronized(lock) {
            val current = result
            if (current == null) {
                listeners.add(listener)
                return
            }
            r = current
        }
        listener(r)
    }

    // --- 변환: 성공 경로 ---------------------------------------------------

    fun <R> map(transform: (T) -> R): Task<R> {
        val next = Task<R>()
        onResult { r -> next.settle(r.mapCatching(transform)) }
        return next
    }

    fun <R> mapAsync(executor: Executor, transform: (T) -> R): Task<R> {
        val next = Task<R>()
        onResult { r -> executor.execute { next.settle(r.mapCatching(transform)) } }
        return next
    }

    fun <R> flatMap(transform: (T) -> Task<R>): Task<R> {
        val next = Task<R>()
        onResult { r -> r.dispatchFlatMap(next, transform) }
        return next
    }

    fun <R> flatMapAsync(executor: Executor, transform: (T) -> Task<R>): Task<R> {
        val next = Task<R>()
        onResult { r ->
            r.fold(
                onSuccess = { executor.execute { r.dispatchFlatMap(next, transform) } },
                onFailure = { e -> next.failure(e) }
            )
        }
        return next
    }

    private fun <R> Result<T>.dispatchFlatMap(next: Task<R>, transform: (T) -> Task<R>) {
        fold(
            onSuccess = { value ->
                val inner = try {
                    transform(value)
                } catch (e: Throwable) {
                    next.failure(e)
                    return
                }
                inner.onResult { innerResult -> next.settle(innerResult) }
            },
            onFailure = { e -> next.failure(e) }
        )
    }

    // --- 변환: 에러 경로 ---------------------------------------------------

    /** 실패를 값으로 복구. */
    fun recover(handler: (Throwable) -> T): Task<T> {
        val next = Task<T>()
        onResult { r -> next.settle(r.recoverCatching(handler)) }
        return next
    }

    fun recoverAsync(executor: Executor, handler: (Throwable) -> T): Task<T> {
        val next = Task<T>()
        onResult { r ->
            if (r.isFailure) executor.execute { next.settle(r.recoverCatching(handler)) }
            else next.settle(r)
        }
        return next
    }

    /** 실패를 다른 Task로 복구 (비동기 복구). */
    fun recoverWith(handler: (Throwable) -> Task<T>): Task<T> {
        val next = Task<T>()
        onResult { r -> r.dispatchRecoverWith(next, handler) }
        return next
    }

    fun recoverWithAsync(executor: Executor, handler: (Throwable) -> Task<T>): Task<T> {
        val next = Task<T>()
        onResult { r ->
            if (r.isFailure) executor.execute { r.dispatchRecoverWith(next, handler) }
            else next.settle(r)
        }
        return next
    }

    private fun Result<T>.dispatchRecoverWith(next: Task<T>, handler: (Throwable) -> Task<T>) {
        fold(
            onSuccess = { next.success(it) },
            onFailure = { e ->
                val recovered = try {
                    handler(e)
                } catch (t: Throwable) {
                    next.failure(t)
                    return
                }
                recovered.onResult { rr -> next.settle(rr) }
            }
        )
    }

    /** 예외를 다른 예외로 변환해 다시 전파. */
    fun mapFailure(transform: (Throwable) -> Throwable): Task<T> {
        val next = Task<T>()
        onResult { r -> next.settle(r.mapFailureCatching(transform)) }
        return next
    }

    fun mapFailureAsync(executor: Executor, transform: (Throwable) -> Throwable): Task<T> {
        val next = Task<T>()
        onResult { r ->
            if (r.isFailure) executor.execute { next.settle(r.mapFailureCatching(transform)) }
            else next.settle(r)
        }
        return next
    }

    private fun Result<T>.mapFailureCatching(transform: (Throwable) -> Throwable): Result<T> =
        recoverCatching { throw transform(it) }

    // --- 부수효과 peek (값/에러 유지, 체인 통과) ----------------------------

    /** 성공 값 들여다보기. 콜백 예외는 실패로 전파. */
    fun onSuccess(action: (T) -> Unit): Task<T> {
        val next = Task<T>()
        onResult { r -> next.settle(r.mapCatching { action(it); it }) }
        return next
    }

    /** 에러 들여다보기. 콜백 예외는 실패로 전파. */
    fun onFailure(action: (Throwable) -> Unit): Task<T> {
        val next = Task<T>()
        onResult { r ->
            val e = r.exceptionOrNull()
            if (e != null) {
                try {
                    action(e)
                } catch (t: Throwable) {
                    next.failure(t)
                    return@onResult
                }
            }
            next.settle(r)
        }
        return next
    }

    /** 성공/실패 무관 종료 시점 콜백. 콜백 예외는 (성공이었다면) 실패로 전파. */
    fun onComplete(action: () -> Unit): Task<T> {
        val next = Task<T>()
        onResult { r ->
            try {
                action()
            } catch (t: Throwable) {
                if (r.isSuccess) {
                    next.failure(t)
                    return@onResult
                }
                // 이미 실패면 원본 실패를 우선 보존
            }
            next.settle(r)
        }
        return next
    }

    // --- 종단 -------------------------------------------------------------

    /** 값을 버리고 완료/에러 신호만 남김. */
    fun then(): Task<Unit> = map { }

    /** 블로킹 대기. 실패 시 원본 예외 전파. (서버용, 메인 스레드 호출 금지) */
    fun get(): T {
        val latch = CountDownLatch(1)
        onResult { latch.countDown() }
        latch.await()
        return resultOrThrow()
    }

    fun get(timeout: Long, unit: TimeUnit): T {
        val latch = CountDownLatch(1)
        onResult { latch.countDown() }
        if (!latch.await(timeout, unit)) {
            throw TimeoutException("Task did not complete within $timeout ${unit.name.lowercase()}")
        }
        return resultOrThrow()
    }

    private fun resultOrThrow(): T {
        val r = synchronized(lock) { checkNotNull(result) }
        return r.getOrThrow()
    }

    companion object {

        /** Executor에 작업 제출. CompletableFuture.supplyAsync 대응. */
        fun <T> async(executor: Executor = TaskExecutors.default(), block: () -> T): Task<T> {
            val task = Task<T>()
            executor.execute { task.settle(runCatching(block)) }
            return task
        }

        fun <T> sync(block: () -> T): Task<T> {
            val task = Task<T>()
            task.settle(runCatching(block))
            return task
        }

        fun <T> wrap(block: () -> Task<T>): Task<T> {
            return sync(block).flatMap { it }
        }

        fun <T> succeed(value: T): Task<T> = Task<T>().apply { success(value) }

        fun <T> failed(e: Throwable): Task<T> = Task<T>().apply { failure(e) }

        /**
         * 여러 Task의 병렬 fan-in. 모두 성공하면 입력 순서대로 값 리스트를 방출하고,
         * 하나라도 실패하면 그 첫 실패로 즉시 완료됨. Promise.all 대응.
         */
        fun <T> all(tasks: List<Task<T>>): Task<List<T>> {
            val next = Task<List<T>>()
            if (tasks.isEmpty()) {
                next.success(emptyList())
                return next
            }
            val slots = AtomicReferenceArray<Any?>(tasks.size)
            val remaining = AtomicInteger(tasks.size)
            tasks.forEachIndexed { i, task ->
                task.onResult { r ->
                    r.fold(
                        onSuccess = { value ->
                            slots.set(i, Slot(value))
                            if (remaining.decrementAndGet() == 0) {
                                val out = ArrayList<T>(tasks.size)
                                for (j in 0 until tasks.size) {
                                    @Suppress("UNCHECKED_CAST")
                                    out.add((slots.get(j) as Slot<T>).value)
                                }
                                next.success(out)
                            }
                        },
                        onFailure = { e -> next.failure(e) } // 첫 실패만 반영, 이후 settle은 무시됨
                    )
                }
            }
            return next
        }

        fun <T> all(vararg tasks: Task<T>): Task<List<T>> {
            return all(tasks.toList())

        }

        /** null 값도 안전하게 담기 위한 래퍼. */
        private class Slot<T>(val value: T)
    }
}

internal fun <T> Executor.task(block: () -> T): Task<T> {
    return Task.async(this, block)
}
