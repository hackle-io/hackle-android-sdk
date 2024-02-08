package io.hackle.android.internal.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expect
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files

class DefaultFileStorageTest {

    private val sdkKey = "abcd1234"
    private var tempDirFile: File? = null
    private var mockContext: Context? = null

    @Before
    fun setup() {
        tempDirFile = Files.createTempDirectory("hackle").toFile()
        tempDirFile!!.mkdirs()
        mockContext = mockk()
        every { mockContext!!.filesDir } returns tempDirFile
    }

    @Test
    fun exists() {
        createFile(File(tempDirFile!!, "hackle/$sdkKey/text.txt"), "abcd1234")
        createFile(File(tempDirFile!!, "hackle/$sdkKey/dir/text.txt"), "abcd1234")
        val storage = DefaultFileStorage(mockContext!!, sdkKey)

        expect {
            that(storage.exists("text.txt")).isTrue()
            that(storage.exists("text1.txt")).isFalse()
            that(storage.exists("dir/text.txt")).isTrue()
            that(storage.exists("dir/text1.txt")).isFalse()
        }
    }

    @Test
    fun write() {
        val storage = DefaultFileStorage(mockContext!!, sdkKey)
        storage.writer("text.txt")
            .use { it.write("abcd1234") }
        storage.writer("dir/text.txt")
            .use { it.write("foobar") }

        expect {
            that(File(tempDirFile!!, "hackle/$sdkKey/text.txt").readText())
                .isEqualTo("abcd1234")
            that(File(tempDirFile!!, "hackle/$sdkKey/dir/text.txt").readText())
                .isEqualTo("foobar")
        }
    }

    @Test
    fun read() {
        createFile(File(tempDirFile!!, "hackle/$sdkKey/text.txt"), "abcd1234")
        createFile(File(tempDirFile!!, "hackle/$sdkKey/dir/text.txt"), "foobar")

        val storage = DefaultFileStorage(mockContext!!, sdkKey)
        expect {
            storage.reader("text.txt").use {
                that(it.readText()).isEqualTo("abcd1234")
            }
            storage.reader("dir/text.txt").use {
                that(it.readText()).isEqualTo("foobar")
            }
        }
    }

    @Test
    fun `read not exists file`() {
        val storage = DefaultFileStorage(mockContext!!, sdkKey)
        expectThrows<Throwable> {
            storage.reader("text.txt")
        }
    }

    @Test
    fun delete() {
        createFile(File(tempDirFile!!, "hackle/$sdkKey/text.txt"), "abcd1234")
        createFile(File(tempDirFile!!, "hackle/$sdkKey/dir/text.txt"), "foobar")
        val storage = DefaultFileStorage(mockContext!!, sdkKey)

        storage.delete("text.txt")
        storage.delete("dir/text.txt")

        expect {
            that(File(tempDirFile!!, "hackle/$sdkKey/text.txt").exists()).isFalse()
            that(File(tempDirFile!!, "hackle/$sdkKey/dir/text.txt").exists()).isFalse()
        }
    }

    private fun createFile(file: File, text: String) {
        val dirFile = file.parentFile!!
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        file.writeText(text)
    }
}