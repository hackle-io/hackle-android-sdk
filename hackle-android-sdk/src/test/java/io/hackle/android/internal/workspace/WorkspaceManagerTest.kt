package io.hackle.android.internal.workspace

import io.hackle.sdk.core.workspace.Workspace
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class WorkspaceManagerTest {

    private lateinit var httpWorkspaceFetcher: HttpWorkspaceFetcher
    private lateinit var sut: WorkspaceManager

    @Before
    fun before() {
        httpWorkspaceFetcher = mockk()
        sut = WorkspaceManager(httpWorkspaceFetcher)
    }

    @Test
    fun `fetch - when before sync then return null`() {
        expectThat(sut.fetch()).isNull()
    }

    @Test
    fun `fetch - when workspace synced then return workspace`() {
        // given
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetchIfModified() } returns workspace

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual).isSameInstanceAs(workspace)
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
    fun `sync - success`() {
        // given
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetchIfModified() } returns workspace

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual).isSameInstanceAs(workspace)
    }

    @Test
    fun `sync - not modified`() {
        // given
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetchIfModified() } returns null

        // when
        sut.sync()
        val actual = sut.fetch()

        // then
        expectThat(actual).isNull()
    }
}