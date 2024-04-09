package io.hackle.android.internal.workspace.repository

import io.hackle.android.internal.storage.FileStorage
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.internal.workspace.WorkspaceConfig
import io.hackle.sdk.core.internal.log.Logger

internal interface WorkspaceConfigRepository {
    fun get(): WorkspaceConfig?
    fun set(value: WorkspaceConfig)
}

internal class DefaultWorkspaceConfigRepository(
    private val fileStorage: FileStorage
) : WorkspaceConfigRepository {

    override fun get(): WorkspaceConfig? {
        if (!fileStorage.exists(FILE_NAME)) {
            return null
        }

        try {
            val reader = fileStorage.reader(FILE_NAME)
            val text = reader.use {
                reader.readText()
            }
            return text.parseJson()
        } catch (_: Exception) {
            fileStorage.delete(FILE_NAME)
        }

        return null
    }

    override fun set(value: WorkspaceConfig) {
        try {
            val writer = fileStorage.writer(FILE_NAME)
            val text = value.toJson()
            writer.use {
                writer.write(text)
                writer.flush()
            }
        } catch (e: Exception) {
            log.debug { "Failed to save workspace config in local storage: $e" }
        }
    }

    companion object {
        private val log = Logger<DefaultWorkspaceConfigRepository>()
        private const val FILE_NAME = "workspace.json"
    }
}