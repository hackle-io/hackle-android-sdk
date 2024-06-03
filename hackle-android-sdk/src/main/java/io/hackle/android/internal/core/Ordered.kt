package io.hackle.android.internal.core

internal interface Ordered {
    val order: Int

    companion object {
        const val HIGHEST = Int.MIN_VALUE
        const val MEDIUM = 0
        const val LOWEST = Int.MAX_VALUE
    }
}

internal data class SimpleOrdered<out T>(
    val value: T,
    override val order: Int
) : Ordered, Comparable<SimpleOrdered<*>> {

    override fun compareTo(other: SimpleOrdered<*>): Int {
        return compareValues(this.order, other.order)
    }

    companion object {
        fun <T> of(value: T, order: Int = Ordered.MEDIUM): SimpleOrdered<T> {
            return SimpleOrdered(value, order)
        }
    }
}
