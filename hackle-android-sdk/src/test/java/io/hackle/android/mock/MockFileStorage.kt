package io.hackle.android.mock

import io.hackle.android.internal.storage.FileStorage
import java.io.File
import java.io.Reader
import java.io.Writer
import java.nio.file.Files

class MockFileStorage : FileStorage {

    val dirFile: File = Files.createTempDirectory("hackle")
        .toFile()

    fun createMockFile(resFilePath: String, dstFilePath: String) {
        val url = javaClass.classLoader!!.getResource(resFilePath)
        File(url.path)
            .copyTo(createFile(dstFilePath))
    }

    fun createFile(filename: String): File =
        File(dirFile, filename)

    override fun exists(filename: String): Boolean =
        createFile(filename)
            .exists()

    override fun writer(filename: String): Writer =
        createFile(filename)
            .bufferedWriter()

    override fun reader(filename: String): Reader =
        createFile(filename)
            .bufferedReader()

    override fun delete(filename: String) {
        createFile(filename).delete()
    }
}