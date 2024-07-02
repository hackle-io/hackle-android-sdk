package io.hackle.android.internal.core

internal data class Updated<out T>(
    val previous: T,
    val current: T
)

internal fun <T, R> Updated<T>.map(transform: (T) -> R): Updated<R> {
    return Updated(transform(previous), transform(current))
}
