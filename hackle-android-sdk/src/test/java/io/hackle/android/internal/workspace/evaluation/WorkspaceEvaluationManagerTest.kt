package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.workspace.evaluation.evaluator.full.FullWorkspaceEvaluateResponse
import io.hackle.android.internal.workspace.evaluation.evaluator.full.FullWorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.evaluator.partial.PartialWorkspaceEvaluateResponse
import io.hackle.android.internal.workspace.evaluation.evaluator.partial.PartialWorkspaceRemoteEvaluator
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluatePolicy
import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.model.DefaultEntity
import io.hackle.sdk.core.model.ServiceType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class WorkspaceEvaluationManagerTest {

    @MockK
    private lateinit var fullEvaluator: FullWorkspaceRemoteEvaluator

    @MockK
    private lateinit var partialEvaluator: PartialWorkspaceRemoteEvaluator

    @MockK
    private lateinit var coreExecutor: Executor

    private lateinit var cache: WorkspaceEvaluationCache
    private lateinit var repository: WorkspaceEvaluationRepository

    private lateinit var sut: WorkspaceEvaluationManager

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { coreExecutor.execute(any()) } answers { firstArg<Runnable>().run() }

        cache = LruWorkspaceEvaluationCache(capacity = 10)
        repository = MockWorkspaceEvaluationRepository()
        sut = WorkspaceEvaluationManager(fullEvaluator, partialEvaluator, repository, cache, coreExecutor)
    }

    private fun user(id: String = "user"): HackleUser {
        return HackleUser.builder().identifier(IdentifierType.ID, id).build()
    }

    @Test
    fun `initialize - 저장된 평가 결과를 캐시에 복원한다`() {
        // given
        val context = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user"))
        repository.set(listOf(context))

        // when
        sut.initialize()

        // then
        expectThat(sut.metadata()) isSameInstanceAs context.workspace.metadata
        expectThat(sut.workspace(user("user"))) isSameInstanceAs context.workspace
    }

    @Test
    fun `initialize - 로드에 실패해도 예외를 던지지 않는다`() {
        // given
        val failingRepository = mockk<WorkspaceEvaluationRepository> {
            every { get() } throws IllegalArgumentException("fail")
        }
        val sut = WorkspaceEvaluationManager(fullEvaluator, partialEvaluator, failingRepository, cache, coreExecutor)

        // when
        sut.initialize()

        // then
        expectThat(sut.metadata()).isNull()
    }

    @Test
    fun `metadata - 가장 최근 평가 결과의 metadata를 리턴한다`() {
        // given
        val context1 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user1"))
        val context2 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user2"))
        cache.put(context1)
        cache.put(context2)

        // when
        val actual = sut.metadata()

        // then
        expectThat(actual) isSameInstanceAs context2.workspace.metadata
    }

    @Test
    fun `metadata - 평가 결과가 없으면 null을 리턴한다`() {
        val actual = sut.metadata()
        expectThat(actual).isNull()
    }

    @Test
    fun `workspace - 유저 식별자 키로 캐시에서 조회한다`() {
        // given
        val context = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user"))
        cache.put(context)

        // when
        val actual = sut.workspace(user("user"))

        // then
        expectThat(actual) isSameInstanceAs context.workspace
    }

    @Test
    fun `workspace - 캐시된 평가 결과가 없으면 null을 리턴한다`() {
        val actual = sut.workspace(user("user"))
        expectThat(actual).isNull()
    }

    @Test
    fun `workspace - session, hackleDeviceId는 키에서 제외하고 조회한다`() {
        // given
        val context = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user"))
        cache.put(context)

        // when: session, hackleDeviceId가 추가되어도 동일한 캐시 항목을 찾는다
        val actual = sut.workspace(
            HackleUser.builder()
                .identifier(IdentifierType.ID, "user")
                .identifier(IdentifierType.SESSION, "session")
                .identifier(IdentifierType.HACKLE_DEVICE_ID, "hackle_device_id")
                .build()
        )

        // then
        expectThat(actual) isSameInstanceAs context.workspace
    }

    @Test
    fun `sync - 캐시된 base로 평가하고 결과를 캐시와 저장소에 반영한다`() {
        // given
        val context = RemoteEvaluateContext.of(user("user"))
        val base = Workspaces.evaluationContext(key = context.key)
        cache.put(base)

        val evaluated = Workspaces.evaluationContext(key = context.key)
        every { fullEvaluator.evaluate(any()) } returns FullWorkspaceEvaluateResponse(evaluated).asFuture()

        // when
        sut.sync(context).get()

        // then
        verify(exactly = 1) {
            fullEvaluator.evaluate(withArg {
                expectThat(it) {
                    get { this.context } isSameInstanceAs context
                    get { this.policy } isEqualTo WorkspaceEvaluatePolicy.AUTO
                    get { this.base } isSameInstanceAs base
                }
            })
        }
        // 평가 결과가 실제 캐시와 저장소에 반영된다
        expectThat(sut.workspace(user("user"))) isSameInstanceAs evaluated.workspace
        expectThat(repository.get()) isEqualTo listOf(evaluated)
    }

    @Test
    fun `sync - base가 없으면 FORCE_FULL로 평가한다`() {
        // given
        val context = RemoteEvaluateContext.of(user("user"))
        val evaluated = Workspaces.evaluationContext(key = context.key)
        every { fullEvaluator.evaluate(any()) } returns FullWorkspaceEvaluateResponse(evaluated).asFuture()

        // when
        sut.sync(context).get()

        // then
        verify(exactly = 1) {
            fullEvaluator.evaluate(withArg {
                expectThat(it) {
                    get { this.policy } isEqualTo WorkspaceEvaluatePolicy.FORCE_FULL
                    get { this.base }.isNull()
                }
            })
        }
        // 평가 결과가 실제 캐시와 저장소에 반영된다
        expectThat(sut.workspace(user("user"))) isSameInstanceAs evaluated.workspace
        expectThat(repository.get()) isEqualTo listOf(evaluated)
    }

    @Test
    fun `sync - 평가 결과 저장은 coreExecutor에서 실행된다`() {
        // given
        val context = RemoteEvaluateContext.of(user("user"))
        val evaluated = Workspaces.evaluationContext(key = context.key)
        every { fullEvaluator.evaluate(any()) } returns FullWorkspaceEvaluateResponse(evaluated).asFuture()

        val store = slot<Runnable>()
        justRun { coreExecutor.execute(capture(store)) }

        // when
        val actual = sut.sync(context)

        // then
        expectThat(actual.isDone) isEqualTo false
        expectThat(sut.workspace(user("user"))).isNull()

        store.captured.run()
        actual.get()
        expectThat(sut.workspace(user("user"))) isSameInstanceAs evaluated.workspace
        expectThat(repository.get()) isEqualTo listOf(evaluated)
    }

    @Test
    fun `sync - 평가에 실패해도 future는 성공으로 완료된다`() {
        // given
        val context = RemoteEvaluateContext.of(user("user"))
        every { fullEvaluator.evaluate(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException("fail"))

        // when
        sut.sync(context).get()

        // then
        expectThat(repository.get()).isEmpty()
    }

    @Test
    fun `evaluate - 엔티티들을 partial 평가해 캐시와 저장소에는 반영하지 않고 결과를 리턴한다`() {
        // given
        val user = user("user")
        val context = RemoteEvaluateContext.of(user)
        val entities = listOf(DefaultEntity(ServiceType.IN_APP_MESSAGE, 320))
        val evaluation = Workspaces.evaluation()

        every { partialEvaluator.evaluate(any()) } returns PartialWorkspaceEvaluateResponse(evaluation).asFuture()

        // when
        val actual = sut.evaluate(context, entities).get()

        // then
        expectThat(actual) isSameInstanceAs evaluation
        verify(exactly = 1) {
            partialEvaluator.evaluate(withArg {
                expectThat(it) {
                    get { this.context } isSameInstanceAs context
                    get { this.entities } isSameInstanceAs entities
                }
            })
        }
        // partial 평가는 결과만 리턴할 뿐 캐시나 저장소에 반영하지 않는다
        expectThat(sut.workspace(user)).isNull()
        expectThat(repository.get()).isEmpty()
    }

    private class MockWorkspaceEvaluationRepository : WorkspaceEvaluationRepository {
        private var contexts: List<WorkspaceEvaluationContext> = emptyList()
        override fun get(): List<WorkspaceEvaluationContext> = contexts
        override fun set(contexts: List<WorkspaceEvaluationContext>) {
            this.contexts = contexts
        }
    }
}
