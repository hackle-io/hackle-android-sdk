package io.hackle.android.internal.utils.concurrent

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ReentrantLocker {
    private val lock = ReentrantLock()
    fun <T> withLock(action: () -> T): T = lock.withLock(action)
}
