package io.hackle.android.internal.workspace.repository

import io.hackle.android.internal.utils.parseJson
import io.hackle.android.internal.workspace.WorkspaceConfig
import io.hackle.android.mock.MockFileStorage
import org.junit.Assert.assertNull
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

class DefaultWorkspaceConfigRepositoryTest {

    @Test
    fun get() {
        val mockFileStorage = MockFileStorage()
        val repository = DefaultWorkspaceConfigRepository(
            fileStorage = mockFileStorage
        )

        val config = repository.get()
        assertNull(config)
    }

    @Test
    fun `should returns saved workspace config data`() {
        val mockFileStorage = MockFileStorage()
        mockFileStorage.createMockFile(
            resFilePath = "workspace_config.json",
            dstFilePath = "workspace.json"
        )
        val repository = DefaultWorkspaceConfigRepository(
            fileStorage = mockFileStorage
        )

        expectThat(repository.get()) {
            get { this?.lastModified } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"
            get { this?.config?.workspace?.id } isEqualTo 7356
            get { this?.config?.workspace?.environment?.id } isEqualTo 112712
        }
    }

    @Test
    fun set() {
        val mockFileStorage = MockFileStorage()
        val repository = DefaultWorkspaceConfigRepository(
            fileStorage = mockFileStorage
        )

        assertNull(repository.get())
        repository.set(createMockWorkspaceConfig("workspace_config.json"))

        expectThat(repository.get()) {
            get { this?.lastModified } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"
            get { this?.config?.workspace?.id } isEqualTo 7356
            get { this?.config?.workspace?.environment?.id } isEqualTo 112712
        }
    }

    @Test
    fun overwrite() {
        val mockFileStorage = MockFileStorage()
        mockFileStorage.createMockFile(
            resFilePath = "workspace_config.json",
            dstFilePath = "workspace.json"
        )
        val repository = DefaultWorkspaceConfigRepository(
            fileStorage = mockFileStorage
        )

        expectThat(repository.get()) {
            get { this?.lastModified } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"
            get { this?.config?.workspace?.id } isEqualTo 7356
            get { this?.config?.workspace?.environment?.id } isEqualTo 112712
        }

        repository.set(createMockWorkspaceConfig("workspace_config_modified.json"))

        expectThat(repository.get()) {
            get { this?.lastModified } isEqualTo "Mon, 22 Jan 2024 08:37:33 GMT"
            get { this?.config?.workspace?.id } isEqualTo 7356
            get { this?.config?.workspace?.environment?.id } isEqualTo 112712
        }
    }

    private fun createMockWorkspaceConfig(filename: String): WorkspaceConfig {
        val url = javaClass.classLoader!!.getResource(filename)
        return File(url.path)
            .readText()
            .parseJson()
    }
}