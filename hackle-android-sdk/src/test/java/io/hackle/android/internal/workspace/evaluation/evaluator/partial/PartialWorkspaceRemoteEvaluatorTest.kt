package io.hackle.android.internal.workspace.evaluation.evaluator.partial

import io.hackle.android.internal.task.asFuture
import io.hackle.android.internal.workspace.evaluation.client.RemoteEvaluateClient
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateResponseDto
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.model.DefaultEntity
import io.hackle.sdk.core.model.ServiceType
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
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class PartialWorkspaceRemoteEvaluatorTest {

    @MockK
    private lateinit var client: RemoteEvaluateClient

    @InjectMockKs
    private lateinit var sut: PartialWorkspaceRemoteEvaluator

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `엔티티들을 평가해 WorkspaceEvaluation으로 리턴한다`() {
        // given
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "user")
            .identifier(IdentifierType.SESSION, "session")
            .build()
        val request = PartialWorkspaceEvaluateRequest(
            context = RemoteEvaluateContext.of(user),
            entities = listOf(
                DefaultEntity(ServiceType.IN_APP_MESSAGE, 320),
                DefaultEntity(ServiceType.AB_TEST, 42),
            )
        )

        val responseDto = EntityEvaluateResponseDto(
            evaluation = Workspaces.entityEvaluationDto(
                workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
                metadata = Workspaces.entityMetadataDto(evaluatedAt = 100, modifiedAt = "42")
            )
        )
        every { client.evaluateEntities(any()) } returns responseDto.asFuture()

        // when
        val actual = sut.evaluate(request).get()

        // then
        verify(exactly = 1) {
            client.evaluateEntities(withArg {
                expectThat(it) {
                    get { context.user.identifiers } isEqualTo mapOf("\$id" to "user", "\$sessionId" to "session")
                    get { entities.map { it.type to it.id } } isEqualTo listOf(
                        "IN_APP_MESSAGE" to 320L,
                        "AB_TEST" to 42L,
                    )
                }
            })
        }
        expectThat(actual.evaluation.metadata) {
            get { id } isEqualTo 1L
            get { environmentId } isEqualTo 2L
            get { evaluatedAt } isEqualTo 100L
            get { modifiedAt } isEqualTo "42"
        }
    }

    @Test
    fun `partial 평가 결과에는 fullEvaluatedAt이 없다`() {
        // given
        val request = PartialWorkspaceEvaluateRequest(
            context = RemoteEvaluateContext.of(HackleUser.builder().identifier(IdentifierType.ID, "user").build()),
            entities = emptyList()
        )
        val responseDto = EntityEvaluateResponseDto(evaluation = Workspaces.entityEvaluationDto())
        every { client.evaluateEntities(any()) } returns responseDto.asFuture()

        // when
        val actual = sut.evaluate(request).get()

        // then
        val evaluation = actual.evaluation as io.hackle.android.internal.workspace.evaluation.DefaultWorkspaceEvaluation
        expectThat(evaluation.fullEvaluatedAt).isNull()
    }
}
