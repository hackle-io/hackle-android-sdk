package io.hackle.android.internal.workspace

import io.hackle.android.internal.lifecycle.AppState.BACKGROUND
import io.hackle.android.internal.lifecycle.AppState.FOREGROUND
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.*
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs


class PollingWorkspaceHandlerTest {

    private lateinit var workspaceCache: WorkspaceCache
    private lateinit var httpWorkspaceFetcher: HttpWorkspaceFetcher
    private lateinit var pollingScheduler: Scheduler

    @Before
    fun before() {
        workspaceCache = spyk(WorkspaceCache())
        httpWorkspaceFetcher = mockk(relaxed = true)
        pollingScheduler = mockk(relaxed = true)
    }

    private fun handler(
        pollingScheduler: Scheduler = this.pollingScheduler,
        pollingIntervalMillis: Long = 60000L,
    ): PollingWorkspaceHandler {
        return PollingWorkspaceHandler(
            workspaceCache = workspaceCache,
            httpWorkspaceFetcher = httpWorkspaceFetcher,
            pollingScheduler = pollingScheduler,
            pollingIntervalMillis = pollingIntervalMillis
        )
    }

    @Test
    fun `initialize - poll`() {
        val sut = handler()
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace
        sut.initialize()
        expectThat(workspaceCache.get()).isSameInstanceAs(workspace)
    }

    @Test
    fun `initialize - start polling`() {
        val sut = handler(
            pollingScheduler = Schedulers.executor("test"),
            pollingIntervalMillis = 200
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace


        sut.initialize()
        Thread.sleep(1100)

        verify(exactly = 6) { // 1 (initialized) + 5 (scheduled)
            workspaceCache.put(any())
        }
    }

    @Test
    fun `initialize - no polling`() {
        val sut = handler(
            pollingScheduler = Schedulers.executor("test"),
            pollingIntervalMillis = -1
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace


        sut.initialize()
        Thread.sleep(1100)

        verify(exactly = 1) { // 1 (initialized)
            workspaceCache.put(any())
        }
    }

    @Test
    fun `onChanged - FOREGROUND start polling`() {
        val sut = handler(
            pollingScheduler = Schedulers.executor("test"),
            pollingIntervalMillis = 200
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace

        sut.onChanged(FOREGROUND, 42)
        Thread.sleep(1100)

        verify(exactly = 5) { // 5 (scheduled)
            workspaceCache.put(any())
        }
    }

    @Test
    fun `onChanged - FOREGROUND no polling`() {
        val sut = handler(
            pollingScheduler = Schedulers.executor("test"),
            pollingIntervalMillis = -1
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace

        sut.onChanged(FOREGROUND, 42)
        Thread.sleep(1100)

        verify { workspaceCache wasNot Called }
    }

    @Test
    fun `onChanged - FOREGROUND start once`() {
        val sut = handler(
            pollingIntervalMillis = 200
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace

        sut.onChanged(FOREGROUND, 42)
        sut.onChanged(FOREGROUND, 42)
        sut.onChanged(FOREGROUND, 42)

        verify(exactly = 1) {
            pollingScheduler.schedulePeriodically(any(), any(), any(), any())
        }
    }

    @Test
    fun `onChanged - BACKGROUND stop polling`() {
        val sut = handler(
            pollingScheduler = Schedulers.executor("test"),
            pollingIntervalMillis = 200
        )
        val workspace = mockk<Workspace>()
        every { httpWorkspaceFetcher.fetch() } returns workspace

        sut.onChanged(FOREGROUND, 42)
        Thread.sleep(500)
        verify(exactly = 2) { workspaceCache.put(any()) }

        sut.onChanged(BACKGROUND, 43)
        Thread.sleep(500)
        verify(exactly = 2) { workspaceCache.put(any()) }
    }
}
