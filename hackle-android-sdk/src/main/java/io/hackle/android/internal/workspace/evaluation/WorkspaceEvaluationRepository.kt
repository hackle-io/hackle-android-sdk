package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.storage.FileStorage
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluationContextDto
import io.hackle.sdk.core.internal.log.Logger

internal interface WorkspaceEvaluationRepository {
    fun get(): List<WorkspaceEvaluationContext>
    fun set(contexts: List<WorkspaceEvaluationContext>)
}

internal class FileWorkspaceEvaluationRepository(
    private val fileStorage: FileStorage
) : WorkspaceEvaluationRepository {
    override fun get(): List<WorkspaceEvaluationContext> {
        try {
            if (!fileStorage.exists(FILE_NAME)) {
                return emptyList()
            }
            val reader = fileStorage.reader(FILE_NAME)
            val json = reader.use { it.readText() }
            val contexts = json.parseJson<List<WorkspaceEvaluationContextDto>>()
            return contexts.map { WorkspaceEvaluationContext.from(it) }
        } catch (e: Exception) {
            log.error { "Failed to read WorkspaceEvaluationContext: $e" }
            try {
                fileStorage.delete(FILE_NAME)
            } catch (_: Exception) {
                // noop
            }
            return emptyList()
        }
    }

    override fun set(contexts: List<WorkspaceEvaluationContext>) {
        try {
            val writer = fileStorage.writer(FILE_NAME)
            val values = contexts.map { it.toDto() }
            val json = values.toJson()
            writer.use {
                it.write(json)
                it.flush()
            }
        } catch (e: Exception) {
            log.error { "Failed to save WorkspaceEvaluationContext: $e" }
        }
    }

    private fun WorkspaceEvaluationContext.toDto(): WorkspaceEvaluationContextDto {
        return WorkspaceEvaluationContextDto(
            key = key.identifiers.asMap(),
            evaluation = dto,
            fullEvaluatedAt = fullEvaluatedAt
        )
    }

    companion object {
        private val log = Logger<FileWorkspaceEvaluationRepository>()
        private const val FILE_NAME = "workspace_evaluation.json"
    }
}
