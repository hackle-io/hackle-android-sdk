package io.hackle.android.internal.workspace

import io.hackle.android.internal.utils.parseJson
import io.hackle.android.mock.MockWorkspaceConfigRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.File

class WorkspaceManagerTest {

    private lateinit var httpWorkspaceFetcher: HttpWorkspaceFetcher
    private lateinit var sut: WorkspaceManager

    @Before
    fun before() {
        httpWorkspaceFetcher = mockk()
        sut = WorkspaceManager(httpWorkspaceFetcher, MockWorkspaceConfigRepository())
    }

    @Test
    fun `fetch - when before sync then return null`() {
        expectThat(sut.fetch()).isNull()
    }

    @Test
    fun `fetch - when workspace synced then return workspace`() {
        // given
        val workspace = createMockWorkspaceConfig("workspace_config.json")
        every { httpWorkspaceFetcher.fetchIfModified() } returns workspace

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual) {
            get { this?.id } isEqualTo workspace.config.workspace.id
            get { this?.environmentId } isEqualTo workspace.config.workspace.environment.id
        }
    }

    @Test
    fun `sync - exception`() {
        // given
        every { httpWorkspaceFetcher.fetchIfModified() } throws IllegalArgumentException()

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `sync - not modified`() {
        // given
        every { httpWorkspaceFetcher.fetchIfModified() } returns null

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual).isNull()
    }

    private fun createMockWorkspaceConfig(filename: String): WorkspaceConfig {
        val url = javaClass.classLoader!!.getResource(filename)
        return File(url.path)
            .readText()
            .parseJson()
    }
}