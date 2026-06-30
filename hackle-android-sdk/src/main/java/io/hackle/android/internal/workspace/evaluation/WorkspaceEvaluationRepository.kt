package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.storage.FileStorage

internal interface WorkspaceEvaluationRepository {
    fun get(): List<WorkspaceEvaluationRecord>
    fun set(records: List<WorkspaceEvaluationRecord>)
}

internal class DefaultWorkspaceEvaluationRepository(
    private val fileStorage: FileStorage
): WorkspaceEvaluationRepository {
    override fun get(): List<WorkspaceEvaluationRecord> {
        TODO("Not yet implemented")
    }

    override fun set(records: List<WorkspaceEvaluationRecord>) {
        TODO("Not yet implemented")
    }
}