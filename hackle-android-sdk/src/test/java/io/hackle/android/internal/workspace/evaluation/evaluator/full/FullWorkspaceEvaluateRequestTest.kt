package io.hackle.android.internal.workspace.evaluation.evaluator.full

import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluatePolicy
import io.hackle.android.support.Workspaces
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class FullWorkspaceEvaluateRequestTest {

    private fun context(): RemoteEvaluateContext {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "user")
            .property("age", 42)
            .hackleProperty("osName", "Android")
            .build()
        return RemoteEvaluateContext.of(user, PropertyOperations.builder().set("grade", "gold").build())
    }

    @Test
    fun `of - base가 없으면 FORCE_FULL로 요청한다`() {
        // when
        val actual = FullWorkspaceEvaluateRequest.of(context(), base = null)

        // then
        expectThat(actual) {
            get { policy } isEqualTo WorkspaceEvaluatePolicy.FORCE_FULL
            get { base }.isNull()
        }
    }

    @Test
    fun `of - base가 있으면 AUTO로 요청한다`() {
        // given
        val base = Workspaces.evaluationContext()

        // when
        val actual = FullWorkspaceEvaluateRequest.of(context(), base)

        // then
        expectThat(actual) {
            get { this.policy } isEqualTo WorkspaceEvaluatePolicy.AUTO
            get { this.base } isSameInstanceAs base
        }
    }

    @Test
    fun `toForceFull - AUTO 요청을 base 없는 FORCE_FULL 요청으로 바꾼다`() {
        // given
        val request = FullWorkspaceEvaluateRequest.of(context(), Workspaces.evaluationContext())

        // when
        val actual = request.toForceFull()

        // then
        expectThat(actual) {
            get { policy } isEqualTo WorkspaceEvaluatePolicy.FORCE_FULL
            get { base }.isNull()
            get { context } isSameInstanceAs request.context
        }
    }

    @Test
    fun `toForceFull - 이미 FORCE_FULL이면 그대로 리턴한다`() {
        // given
        val request = FullWorkspaceEvaluateRequest.of(context(), base = null)

        // when
        val actual = request.toForceFull()

        // then
        expectThat(actual) isSameInstanceAs request
    }

    @Test
    fun `toDto - policy, user, operations, base를 매핑한다`() {
        // given
        val base = Workspaces.evaluationContext(
            evaluation = Workspaces.evaluationDto(
                results = listOf(Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11))
            ),
            fullEvaluatedAt = 100
        )
        val request = FullWorkspaceEvaluateRequest.of(context(), base)

        // when
        val actual = request.toDto()

        // then
        expectThat(actual) {
            get { policy } isEqualTo "AUTO"
            get { context.user.identifiers } isEqualTo mapOf("\$id" to "user")
            get { context.user.userProperties } isEqualTo mapOf<String, Any>("age" to 42)
            get { context.user.hackleProperties } isEqualTo mapOf<String, Any>("osName" to "Android")
            get { context.operations } isEqualTo mapOf("\$set" to mapOf<String, Any>("grade" to "gold"))
        }
        expectThat(actual.base).isNotNull().and {
            get { fullEvaluatedAt } isEqualTo 100L
            get { entities.map { Triple(it.type, it.id, it.hash) } } isEqualTo listOf(Triple("AB_TEST", 1L, 11))
        }
    }

    @Test
    fun `toDto - base가 없으면 base를 매핑하지 않는다`() {
        // given
        val request = FullWorkspaceEvaluateRequest.of(context(), base = null)

        // when
        val actual = request.toDto()

        // then
        expectThat(actual) {
            get { policy } isEqualTo "FORCE_FULL"
            get { base }.isNull()
        }
    }
}
