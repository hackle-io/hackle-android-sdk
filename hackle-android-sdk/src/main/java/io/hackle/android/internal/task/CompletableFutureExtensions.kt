package io.hackle.android.internal.task

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal object Futures {

    inline fun <T> ui(crossinline block: () -> T): CompletableFuture<T> {
        return async(TaskExecutors.main(), block)
    }

    inline fun <T> async(
        executor: Executor,
        crossinline block: () -> T
    ): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ block() }, executor)
    }

    inline fun <T> sync(block: () -> T): CompletableFuture<T> {
        return try {
            block().asFuture()
        } catch (ex: Throwable) {
            CompletableFuture<T>().apply { completeExceptionally(ex) }
        }
    }

    inline fun <T> wrap(block: () -> CompletableFuture<T>): CompletableFuture<T> {
        return try {
            block()
        } catch (ex: Throwable) {
            CompletableFuture<T>().apply { completeExceptionally(ex) }
        }
    }

    fun allOf(futures: List<CompletableFuture<*>>): CompletableFuture<Void> {
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    fun completed(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}

internal inline fun <T> Executor.future(crossinline block: () -> T): CompletableFuture<T> {
    return Futures.async(this, block)
}

internal fun <T> T.asFuture(): CompletableFuture<T> {
    return CompletableFuture.completedFuture(this)
}

internal inline fun <T, R> CompletableFuture<T>.map(crossinline transform: (T) -> R): CompletableFuture<R> {
    return thenApply { transform(it) }
}


internal inline fun <T, R> CompletableFuture<T>.mapAsync(
    executor: Executor,
    crossinline transform: (T) -> R
): CompletableFuture<R> {
    return thenApplyAsync({ transform(it) }, executor)
}

internal inline fun <T> CompletableFuture<T>.consume(crossinline action: (T) -> Unit): CompletableFuture<Void> {
    return thenAccept { action(it) }
}

internal inline fun <T> CompletableFuture<T>.consumeAsync(
    executor: Executor,
    crossinline action: (T) -> Unit
): CompletableFuture<Void> {
    return thenAcceptAsync({ action(it) }, executor)
}

internal inline fun <T, R> CompletableFuture<T>.flatMap(crossinline transform: (T) -> CompletableFuture<R>): CompletableFuture<R> {
    return thenCompose { transform(it) }
}

internal inline fun <T, R> CompletableFuture<T>.flatMapAsync(
    executor: Executor,
    crossinline transform: (T) -> CompletableFuture<R>
): CompletableFuture<R> {
    return thenComposeAsync({ transform(it) }, executor)
}

internal inline fun <T> CompletableFuture<T>.recover(crossinline transform: (Throwable) -> T): CompletableFuture<T> {
    return exceptionally { transform(it) }
}

@JvmName("recoverVoid")
internal inline fun CompletableFuture<Void>.recover(crossinline action: (Throwable) -> Unit): CompletableFuture<Void> {
    return exceptionally {
        action(it)
        null
    }
}

internal inline fun <T> CompletableFuture<T>.onSuccess(crossinline action: (T) -> Unit): CompletableFuture<T> {
    return whenComplete { value, error ->
        if (error == null) action(value)
    }
}

internal inline fun <T> CompletableFuture<T>.onSuccessAsync(
    executor: Executor,
    crossinline action: (T) -> Unit
): CompletableFuture<T> {
    return whenCompleteAsync({ value, error ->
        if (error == null) action(value)
    }, executor)
}

internal inline fun <T> CompletableFuture<T>.onFailure(crossinline action: (Throwable) -> Unit): CompletableFuture<T> {
    return whenComplete { _, error ->
        if (error != null) action(error)
    }
}

internal inline fun <T> CompletableFuture<T>.onComplete(crossinline action: () -> Unit): CompletableFuture<T> {
    return whenComplete { _, _ -> action() }
}


internal inline fun <T> CompletableFuture<T>.onCompleteAsync(
    executor: Executor,
    crossinline action: () -> Unit
): CompletableFuture<T> {
    return whenCompleteAsync({ _, _ -> action() }, executor)
}
