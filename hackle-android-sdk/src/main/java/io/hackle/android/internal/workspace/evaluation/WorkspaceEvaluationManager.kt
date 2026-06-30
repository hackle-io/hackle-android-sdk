package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.task.Task
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.android.internal.workspace.evaluation.evaluator.AllWorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.evaluator.SpecificWorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateProcessor
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluateResponse
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateStatus
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.Entity
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluationFetcher

internal class WorkspaceEvaluationManager(
    private val evaluateProcessor: WorkspaceEvaluateProcessor,
    private val repository: WorkspaceEvaluationRepository,
    private val cache: WorkspaceEvaluationCache,
) : WorkspaceManager, WorkspaceEvaluationFetcher {

    override fun initialize() {
        load()
    }

    override fun metadata(): Workspace.Metadata? {
        return cache.latest()?.workspace()?.metadata
    }

    override fun workspace(user: HackleUser): WorkspaceEvaluation? {
        val key = WorkspaceEvaluationContext.keyOf(user)
        return cache.get(key)?.workspace()
    }

    fun sync(context: WorkspaceEvaluationContext): Task<Unit> {
        val record = cache.get(context.key)
        val request = AllWorkspaceEvaluateRequest(context, record)
        return evaluateProcessor.process(request)
            .map { resolveResponse(request, it) }
            .map { store(it) }
            .recover { log.error { "Failed to sync WorkspaceEvaluation: $it" } }
    }

    private fun resolveResponse(
        request: AllWorkspaceEvaluateRequest,
        response: WorkspaceEvaluateResponse,
    ): WorkspaceEvaluationRecord {
        return when (response.status) {
            WorkspaceEvaluateStatus.FULL -> WorkspaceEvaluationRecord.from(
                request.context.key,
                requireNotNull(response.evaluation) { "response evaluation" })

            WorkspaceEvaluateStatus.DELTA -> requireNotNull(request.record) { "current record" } // 실제 발생하지 않지만 방어 로직
            WorkspaceEvaluateStatus.NOT_MODIFIED -> requireNotNull(request.record) { "current record" }
        }
    }

    private fun store(record: WorkspaceEvaluationRecord) {
        val snapshots = cache.put(record)
        repository.set(snapshots)
    }

    private fun load() {
        try {
            val records = repository.get()
            cache.restore(records)
        } catch (e: Exception) {
            log.error { "Failed to load WorkspaceEValuation from local: $e" }
        }
    }

    fun evaluate(context: WorkspaceEvaluationContext, entities: List<Entity>): Task<WorkspaceEvaluation> {
        val request = SpecificWorkspaceEvaluateRequest(context, entities)
        return evaluateProcessor.process(request)
            .map { DefaultWorkspaceEvaluation.from(requireNotNull(it.evaluation) { "evaluation" }) }
    }

    companion object {
        private val log = Logger<WorkspaceEvaluationManager>()
    }
}
