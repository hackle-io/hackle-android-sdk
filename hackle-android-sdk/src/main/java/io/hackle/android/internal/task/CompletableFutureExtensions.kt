package io.hackle.android.internal.task

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor


internal inline fun <T> TaskExecutors.future(
    executor: Executor = default(),
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

internal inline fun <T> CompletableFuture<T>.onSuccess(crossinline action: (T) -> Unit): CompletableFuture<T> {
    return whenComplete { value, error ->
        if (error == null) action(value)
    }
}

internal inline fun <T> CompletableFuture<T>.onFailure(crossinline action: (Throwable) -> Unit): CompletableFuture<T> {
    return whenComplete { _, error ->
        if (error != null) action(error)
    }
}

internal inline fun <T> CompletableFuture<T>.onComplete(crossinline action: () -> Unit): CompletableFuture<T> {
    return whenComplete { _, _ -> action() }
}

internal object CompletableFutures {
    fun allOf(futures: List<CompletableFuture<*>>): CompletableFuture<Void> {
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    fun void(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}
