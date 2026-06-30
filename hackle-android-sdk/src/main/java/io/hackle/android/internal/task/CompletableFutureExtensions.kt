package io.hackle.android.internal.task

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService


internal inline fun <T> TaskExecutors.future(
    executor: ExecutorService = default(),
    crossinline block: () -> T
): CompletableFuture<T> {
    return executor.future(block)
}

internal inline fun <T> Executor.future(crossinline block: () -> T): CompletableFuture<T> {
    return CompletableFuture.supplyAsync({ block() }, this)
}

internal fun <T> T.asFuture(): CompletableFuture<T> {
    return CompletableFuture.completedFuture(this)
}

internal inline fun <T, R> CompletableFuture<T>.map(crossinline transform: (T) -> R): CompletableFuture<R> {
    return thenApply { transform(it) }
}

internal inline fun <T> CompletableFuture<T>.consume(crossinline action: (T) -> Unit): CompletableFuture<Void> {
    return thenAccept { action(it) }
}

internal inline fun <T, R> CompletableFuture<T>.flatMap(crossinline transform: (T) -> CompletableFuture<R>): CompletableFuture<R> {
    return thenCompose { transform(it) }
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

internal object CompletableFutures {
    fun allOf(futures: List<CompletableFuture<*>>): CompletableFuture<Void> {
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    fun void(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}
