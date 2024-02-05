package io.hackle.android.internal.storage

import android.content.Context
import java.io.File
import java.io.Reader
import java.io.Writer

internal interface FileStorage {
    fun exists(filename: String): Boolean
    fun writer(filename: String): Writer
    fun reader(filename: String): Reader
    fun delete(filename: String)
}

internal class DefaultFileStorage(
    context: Context,
    sdkKey: String
): FileStorage {
    private val dirFile: File = File(context.filesDir, "$ROOT_DIR_NAME/$sdkKey")
            .apply {
                if (!exists()) {
                    mkdirs()
                }
            }

    private fun createFile(filename: String): File =
        File(dirFile, filename)

    override fun exists(filename: String): Boolean =
        createFile(filename)
            .exists()

    override fun writer(filename: String): Writer {
        val targetFile = createFile(filename)
        val parentFile = targetFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs()
        }

        return createFile(filename)
            .bufferedWriter()
    }

    override fun reader(filename: String): Reader =
        createFile(filename)
            .bufferedReader()

    override fun delete(filename: String) {
        createFile(filename).delete()
    }

    companion object {
        private const val ROOT_DIR_NAME = "hackle"
    }
}