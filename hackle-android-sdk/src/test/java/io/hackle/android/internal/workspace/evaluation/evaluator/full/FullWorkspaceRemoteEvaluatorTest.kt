package io.hackle.android.internal.workspace.evaluation.evaluator.full

import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.workspace.evaluation.client.RemoteEvaluateClient
import io.hackle.android.internal.workspace.evaluation.evaluator.WorkspaceEvaluations
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateResponseDto
import io.hackle.android.support.Workspaces
import io.hackle.android.support.assertThrows
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class FullWorkspaceRemoteEvaluatorTest {

    @MockK
    private lateinit var client: RemoteEvaluateClient

    @InjectMockKs
    private lateinit var sut: FullWorkspaceRemoteEvaluator

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    private fun context(): RemoteEvaluateContext {
        return RemoteEvaluateContext.of(
            HackleUser.builder().identifier(IdentifierType.ID, "user").build()
        )
    }

    @Test
    fun `변경되지 않았으면 base 컨텍스트를 그대로 리턴한다`() {
        // given
        val base = Workspaces.evaluationContext()
        val request = FullWorkspaceEvaluateRequest.of(context(), base)
        every { client.evaluateIfModified(any()) } returns CompletableFuture.completedFuture(null)

        // when
        val actual = sut.evaluate(request).get()

        // then
        expectThat(actual.context) isSameInstanceAs base
    }

    @Test
    fun `변경되지 않았는데 base가 없으면 예외로 완료된다`() {
        // given
        val request = FullWorkspaceEvaluateRequest.of(context(), base = null)
        every { client.evaluateIfModified(any()) } returns CompletableFuture.completedFuture(null)

        // when
        val exception = assertThrows<ExecutionException> {
            sut.evaluate(request).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalArgumentException>()
    }

    @Test
    fun `FULL - 전체 평가 결과로 새 컨텍스트를 만든다`() {
        // given
        val request = FullWorkspaceEvaluateRequest.of(context(), base = null)
        val full = Workspaces.evaluationDto(
            metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100),
            results = listOf(Workspaces.resultDto(id = 1, hash = 11))
        )
        val responseDto = WorkspaceEvaluateResponseDto(
            status = "FULL",
            full = full,
            delta = null
        )
        every { client.evaluateIfModified(any()) } returns responseDto.asFuture()

        // when
        val actual = sut.evaluate(request).get()

        // then
        expectThat(actual.context) {
            get { key } isEqualTo request.context.key
            get { dto } isSameInstanceAs full
            get { fullEvaluatedAt } isEqualTo 100L
        }
    }

    @Test
    fun `DELTA - base에 변경분을 병합한 컨텍스트를 만든다`() {
        // given
        val result1 = Workspaces.resultDto(id = 1, hash = 11)
        val result2 = Workspaces.resultDto(id = 2, hash = 22)
        val base = Workspaces.evaluationContext(
            evaluation = Workspaces.evaluationDto(results = listOf(result1, result2)),
            fullEvaluatedAt = 100
        )
        val request = FullWorkspaceEvaluateRequest.of(context(), base)

        val result2Changed = Workspaces.resultDto(id = 2, hash = 222)
        val mergedHash = WorkspaceEvaluations.hash(listOf(result1, result2Changed))
        val delta = Workspaces.deltaDto(
            metadata = Workspaces.evaluationMetadataDto(hash = mergedHash, evaluatedAt = 200),
            changed = listOf(result2Changed)
        )
        val responseDto = WorkspaceEvaluateResponseDto(
            status = "DELTA",
            full = null,
            delta = delta
        )
        every { client.evaluateIfModified(any()) } returns responseDto.asFuture()

        // when
        val actual = sut.evaluate(request).get()

        // then
        expectThat(actual.context) {
            get { key } isEqualTo base.key
            get { fullEvaluatedAt } isEqualTo 100L
            get { dto.results } isEqualTo listOf(result1, result2Changed)
            get { dto.metadata } isSameInstanceAs delta.metadata
        }
        verify(exactly = 1) { client.evaluateIfModified(any()) }
    }

    @Test
    fun `DELTA - 병합 해시가 일치하지 않으면 FORCE_FULL로 다시 평가한다`() {
        // given
        val base = Workspaces.evaluationContext(
            evaluation = Workspaces.evaluationDto(results = listOf(Workspaces.resultDto(id = 1, hash = 11)))
        )
        val request = FullWorkspaceEvaluateRequest.of(context(), base)

        val delta = Workspaces.deltaDto(
            metadata = Workspaces.evaluationMetadataDto(hash = -1),
            changed = listOf(Workspaces.resultDto(id = 2, hash = 22))
        )
        val full = Workspaces.evaluationDto(metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 300))

        val requests = mutableListOf<WorkspaceEvaluateRequestDto>()
        val responseDto1 = WorkspaceEvaluateResponseDto(
            status = "DELTA",
            full = null,
            delta = delta
        )
        val responseDto2 = WorkspaceEvaluateResponseDto(
            status = "FULL",
            full = full,
            delta = null
        )
        every { client.evaluateIfModified(capture(requests)) } returnsMany listOf(
            responseDto1.asFuture(),
            responseDto2.asFuture()
        )

        // when
        val actual = sut.evaluate(request).get()

        // then
        verify(exactly = 2) { client.evaluateIfModified(any()) }
        expectThat(requests[0].policy) isEqualTo "AUTO"
        expectThat(requests[1]) {
            get { policy } isEqualTo "FORCE_FULL"
            get { this.base } isEqualTo null
        }
        expectThat(actual.context) {
            get { dto } isSameInstanceAs full
            get { fullEvaluatedAt } isEqualTo 300L
        }
    }
}
