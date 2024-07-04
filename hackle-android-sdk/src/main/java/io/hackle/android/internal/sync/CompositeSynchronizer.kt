package io.hackle.android.internal.sync

import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class CompositeSynchronizer(private val executor: ExecutorService) : Synchronizer {

    private val synchronizers = CopyOnWriteArrayList<Synchronizer>()

    fun add(synchronizer: Synchronizer) {
        synchronizers.add(synchronizer)
        log.debug { "Synchronizer added [${synchronizer::class.java.simpleName}]" }
    }

    override fun sync() {
        val jobs = mutableListOf<SyncJob>()
        for (synchronizer in synchronizers) {
            try {
                val future = executor.submit { synchronizer.sync() }
                jobs.add(SyncJob(synchronizer, future))
            } catch (e: Exception) {
                log.error { "Failed to sync $synchronizer: $e" }
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
                log.error { "Failed to sync ${synchronizer}: $e" }
            }
        }
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
