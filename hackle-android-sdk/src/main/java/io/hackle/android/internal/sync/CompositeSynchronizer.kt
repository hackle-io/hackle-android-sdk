package io.hackle.android.internal.sync

import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

enum class SynchronizerType {
    WORKSPACE,
    COHORT
}

internal class Synchronization(
    val type: SynchronizerType,
    val synchronizer: Synchronizer
)

internal class CompositeSynchronizer(private val executor: ExecutorService) : Synchronizer {

    private val synchronizations = CopyOnWriteArrayList<Synchronization>()

    fun add(type: SynchronizerType, synchronizer: Synchronizer) {
        synchronizations.add(Synchronization(type, synchronizer))
        log.debug { "Synchronizer added [${synchronizer::class.java.simpleName}]" }
    }

    override fun sync() {
        val jobs = mutableListOf<SyncJob>()
        for (synchronization in synchronizations) {
            try {
                val future = executor.submit { synchronization.synchronizer.sync() }
                jobs.add(SyncJob(synchronization, future))
            } catch (e: Exception) {
                log.error { "Failed to sync $synchronization: $e" }
            }
        }
        jobs.forEach { it.await() }
    }

    fun sync(type: SynchronizerType) {
        val synchronization = synchronizations.find { it.type == type }
        requireNotNull(synchronization) { "Unsupported SynchronizerType [$type]" }
        synchronization.synchronizer.sync()
    }

    class SyncJob(
        private val synchronization: Synchronization,
        private val future: Future<*>
    ) {
        fun await() {
            try {
                future.get()
            } catch (e: Exception) {
                log.error { "Failed to sync ${synchronization.synchronizer}: $e" }
            }
        }
    }

    override fun toString(): String {
        return synchronizations.joinToString(
            separator = ", ",
            prefix = "CompositeSynchronizer(",
            postfix = ")"
        ) { it::class.java.simpleName }
    }

    companion object {
        private val log = Logger<CompositeSynchronizer>()
    }
}
