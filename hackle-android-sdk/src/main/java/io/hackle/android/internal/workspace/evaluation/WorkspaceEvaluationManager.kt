package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.task.consumeAsync
import io.hackle.android.internal.task.map
import io.hackle.android.internal.task.recover
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.android.internal.workspace.evaluation.evaluator.full.FullWorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.evaluator.full.FullWorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.evaluator.partial.PartialWorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.evaluator.partial.PartialWorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.Entity
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluationFetcher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal class WorkspaceEvaluationManager(
    private val fullEvaluator: FullWorkspaceRemoteEvaluator,
    private val partialEvaluator: PartialWorkspaceRemoteEvaluator,
    private val repository: WorkspaceEvaluationRepository,
    private val cache: WorkspaceEvaluationCache,
    private val executor: Executor,
) : WorkspaceManager, WorkspaceEvaluationFetcher {

    override fun initialize() {
        load()
    }

    override fun metadata(): Workspace.Metadata? {
        return cache.latest()?.workspace?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceEvaluation? {
        val key = WorkspaceEvaluationContext.keyOf(user)
        return cache.get(key)?.workspace
    }

    fun sync(context: RemoteEvaluateContext): CompletableFuture<Void> {
        val base = cache.get(context.key)
        val request = FullWorkspaceEvaluateRequest.of(context, base)
        return fullEvaluator.evaluate(request)
            .consumeAsync(executor) { store(it.context) }
            .recover { log.error { "Failed to sync WorkspaceEvaluation: $it" } }
    }

    private fun store(context: WorkspaceEvaluationContext) {
        val snapshots = cache.put(context)
        repository.set(snapshots)
    }

    private fun load() {
        try {
            val context = repository.get()
            cache.restore(context)
        } catch (e: Exception) {
            log.error { "Failed to load WorkspaceEvaluation from local: $e" }
        }
    }

    fun evaluate(context: RemoteEvaluateContext, entities: List<Entity>): CompletableFuture<WorkspaceEvaluation> {
        val request = PartialWorkspaceEvaluateRequest(context, entities)
        return partialEvaluator.evaluate(request)
            .map { it.evaluation }
    }

    companion object {
        private val log = Logger<WorkspaceEvaluationManager>()
    }
}
