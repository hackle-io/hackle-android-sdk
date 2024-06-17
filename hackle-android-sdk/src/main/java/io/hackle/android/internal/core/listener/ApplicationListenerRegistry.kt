package io.hackle.android.internal.core.listener

import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.core.SimpleOrdered
import java.util.concurrent.atomic.AtomicReference

internal abstract class ApplicationListenerRegistry<T : ApplicationListener> {

    private val lock = Any()

    private val ordered = mutableListOf<SimpleOrdered<T>>()
    private val cache = AtomicReference<List<T>>(emptyList())

    val listeners: List<T> get() = cache.get()

    fun addListener(listener: T, order: Int = Ordered.MEDIUM) {
        synchronized(lock) {
            ordered.add(SimpleOrdered.of(listener, order))
            ordered.sort()
            val listeners = ordered.map { it.value }
            cache.set(listeners)
        }
    }
}
