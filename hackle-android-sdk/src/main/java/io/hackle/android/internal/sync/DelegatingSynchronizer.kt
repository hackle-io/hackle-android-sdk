package io.hackle.android.internal.sync

import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class DelegatingSynchronizer(private val executor: ExecutorService) : Synchronizer {

    private val delegates = CopyOnWriteArrayList<Synchronizer>()

    fun add(synchronizer: Synchronizer) {
        delegates.add(synchronizer)
        log.debug { "Synchronizer added [${synchronizer::class.java.simpleName}]" }
    }

    override fun sync() {
        val jobs = mutableListOf<SyncJob>()
        for (delegate in delegates) {
            try {
                val future = executor.submit { delegate.sync() }
                jobs.add(SyncJob(delegate, future))
            } catch (e: Exception) {
                log.error { "Failed to sync $delegate: $e" }
            }
        }
        jobs.forEach { it.await() }
    }

    class SyncJob(
        private val synchronizer: Synchronizer,
        private val future: Future<*>
    ) {
        fun await() {
            try {
                future.get()
            } catch (e: Exception) {
                log.error { "Failed to sync $synchronizer: $e" }
            }
        }
    }

    override fun toString(): String {
        return delegates.joinToString(
            separator = ", ",
            prefix = "DelegatingSynchronizer(",
            postfix = ")"
        ) { it::class.java.simpleName }
    }

    companion object {
        private val log = Logger<DelegatingSynchronizer>()
    }
}
