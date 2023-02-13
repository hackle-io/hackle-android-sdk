package io.hackle.android.internal.workspace

import io.hackle.sdk.core.workspace.Workspace
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class WorkspaceCacheHandlerTest {
    @Test
    fun `가져온 Workpsace 를 캐시에 넣는다`() {
        val workspace = mockk<Workspace>()
        val workspaceFetcher = mockk<HttpWorkspaceFetcher> {
            every { fetch() } returns workspace
        }
        val cache = WorkspaceCache()
        val cacheHandler = WorkspaceCacheHandler(cache, workspaceFetcher)
        expectThat(cache.get()).isNull()
        cacheHandler.fetchAndCache()
        expectThat(cache.get()) isEqualTo workspace
    }

    @Test
    fun `Workspace를 가져오다 에러가 나면 무시한다`() {
        val workspaceFetcher = mockk<HttpWorkspaceFetcher> {
            every { fetch() } throws IllegalArgumentException()
        }
        val cache = WorkspaceCache()
        val cacheHandler = WorkspaceCacheHandler(cache, workspaceFetcher)
        expectThat(cache.get()).isNull()
        cacheHandler.fetchAndCache()
        expectThat(cache.get()).isNull()
    }
}