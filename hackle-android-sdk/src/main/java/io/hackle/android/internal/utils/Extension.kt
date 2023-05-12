package io.hackle.android.internal.utils

inline fun <T> Iterable<T>.anyOrEmpty(predicate: (T) -> Boolean): Boolean{
    if (this is Collection && isEmpty()) return true
    for (element in this) if (predicate(element)) return true
    return false
}