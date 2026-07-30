package io.hackle.android.internal.sync

import io.hackle.android.internal.task.Futures
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

internal class CompositeSynchronizer : Synchronizer {

    private val synchronizers = CopyOnWriteArrayList<Synchronizer>()

    fun add(synchronizer: Synchronizer) {
        synchronizers.add(synchronizer)
        log.debug { "Synchronizer added [${synchronizer::class.java.simpleName}]" }
    }

    override fun sync(): CompletableFuture<Void> {
        val futures = synchronizers.map { it.safeSync() }
        return Futures.allOf(futures)
    }

    override fun toString(): String {
        return synchronizers.joinToString(
            separator = ", ",
            prefix = "CompositeSynchronizer(",
            postfix = ")"
        ) { it::class.java.simpleName }
    }

    companion object {
        private val log = Logger<CompositeSynchronizer>()
    }
}
