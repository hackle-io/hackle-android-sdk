package io.hackle.android.internal.workspace.config

import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture

class WorkspaceConfigManagerTest {

    @MockK
    private lateinit var fetcher: HttpWorkspaceConfigFetcher

    private lateinit var repository: WorkspaceConfigRepository
    private lateinit var sut: WorkspaceConfigManager

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repository = MockWorkspaceConfigRepository()
        sut = WorkspaceConfigManager(fetcher, repository)
    }

    @Test
    fun `initialize - 저장된 WorkspaceConfig를 로드한다`() {
        // given
        val context = Workspaces.configContext(modifiedAt = "42")
        repository.set(context)

        // when
        sut.initialize()

        // then
        expectThat(sut.metadata()) isSameInstanceAs context.workspace.metadata
        expectThat(sut.workspace(user())) isSameInstanceAs context.workspace
    }

    @Test
    fun `initialize - 저장된 WorkspaceConfig가 없으면 로드하지 않는다`() {
        // when
        sut.initialize()

        // then
        expectThat(sut.metadata()).isNull()
        expectThat(sut.workspace(user())).isNull()
    }

    @Test
    fun `initialize - 로드에 실패해도 예외를 던지지 않는다`() {
        // given
        val failingRepository = mockk<WorkspaceConfigRepository> {
            every { get() } throws IllegalArgumentException("fail")
        }
        val sut = WorkspaceConfigManager(fetcher, failingRepository)

        // when
        sut.initialize()

        // then
        expectThat(sut.workspace(user())).isNull()
    }

    @Test
    fun `sync - 새로운 WorkspaceConfig를 메모리와 저장소에 반영한다`() {
        // given
        val context = Workspaces.configContext(modifiedAt = "42")
        every { fetcher.fetchIfModified(any()) } returns CompletableFuture.completedFuture(context)

        // when
        sut.sync().get()

        // then
        expectThat(sut.workspace(user())) isSameInstanceAs context.workspace
        expectThat(repository.get()) isSameInstanceAs context
        verify(exactly = 1) { fetcher.fetchIfModified(null) }
    }

    @Test
    fun `sync - 로드된 config의 modifiedAt으로 변경 여부를 확인한다`() {
        // given
        repository.set(Workspaces.configContext(modifiedAt = "42"))
        every { fetcher.fetchIfModified(any()) } returns CompletableFuture.completedFuture(null)
        sut.initialize()

        // when
        sut.sync().get()

        // then
        verify(exactly = 1) { fetcher.fetchIfModified("42") }
    }

    @Test
    fun `sync - 변경되지 않았으면 기존 config를 유지하고 저장하지 않는다`() {
        // given
        val context = Workspaces.configContext(modifiedAt = "42")
        repository.set(context)
        every { fetcher.fetchIfModified(any()) } returns CompletableFuture.completedFuture(null)
        sut.initialize()

        // when
        sut.sync().get()

        // then
        expectThat(sut.workspace(user())) isSameInstanceAs context.workspace
        expectThat(repository.get()) isSameInstanceAs context
    }

    @Test
    fun `sync - fetch에 실패해도 future는 성공으로 완료된다`() {
        // given
        val context = Workspaces.configContext(modifiedAt = "42")
        repository.set(context)
        val failedFuture = CompletableFuture<WorkspaceConfigContext?>()
        failedFuture.completeExceptionally(IllegalArgumentException("fail"))
        every { fetcher.fetchIfModified(any()) } returns failedFuture
        sut.initialize()

        // when
        sut.sync().get()

        // then
        expectThat(sut.workspace(user())) isSameInstanceAs context.workspace
    }

    @Test
    fun `workspace - user와 무관하게 동일한 config를 리턴한다`() {
        // given
        val context = Workspaces.configContext(modifiedAt = "42")
        repository.set(context)
        sut.initialize()

        // when
        val actual1 = sut.workspace(user("user1"))
        val actual2 = sut.workspace(user("user2"))

        // then
        expectThat(actual1) isSameInstanceAs context.workspace
        expectThat(actual2) isSameInstanceAs context.workspace
    }

    private fun user(id: String = "user"): HackleUser {
        return HackleUser.builder().identifier(IdentifierType.ID, id).build()
    }

    private class MockWorkspaceConfigRepository : WorkspaceConfigRepository {
        private var value: WorkspaceConfigContext? = null
        override fun get(): WorkspaceConfigContext? = value
        override fun set(record: WorkspaceConfigContext) {
            value = record
        }
    }
}
