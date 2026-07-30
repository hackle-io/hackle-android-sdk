package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.android.support.InAppMessages
import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.EvaluationPhase
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.DELIVER
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.TRIGGER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.local.InAppMessageEligibilityLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.remote.InAppMessageEligibilityRemoteEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.local.InAppMessageLayoutLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.remote.InAppMessageLayoutRemoteEvaluateRequest
import io.hackle.sdk.core.model.PlatformType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

class InAppMessageEvaluatorsTest {

    @MockK
    private lateinit var evaluateProcessor: EvaluateProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    private fun user(): HackleUser {
        return HackleUser.builder().identifier(IdentifierType.ID, "user").build()
    }

    @Test
    fun `eligibility(Local) - LOCAL eligibility 요청을 조립해 평가한다`() {
        // given
        val workspace = Workspaces.config()
        val inAppMessage = InAppMessages.config()
        val user = user()
        val response = InAppMessages.eligibilityResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns response

        // when
        val actual = evaluateProcessor.eligibility(workspace, inAppMessage, user, TRIGGER, timestamp = 42)

        // then
        expectThat(actual) isSameInstanceAs response
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageEligibilityEvaluateRequest> {
                expectThat(it).isA<InAppMessageEligibilityLocalEvaluateRequest>().and {
                    get { this.workspace } isSameInstanceAs workspace
                    get { entity } isSameInstanceAs inAppMessage
                    get { this.user } isSameInstanceAs user
                    get { scope } isEqualTo TRIGGER
                    get { platformType } isEqualTo PlatformType.ANDROID
                    get { timestamp } isEqualTo 42L
                    get { record } isEqualTo true
                    get { phase } isEqualTo EvaluationPhase.RUNTIME
                }
            })
        }
    }

    @Test
    fun `layout(Local) - LOCAL layout 요청을 조립해 평가한다`() {
        // given
        val workspace = Workspaces.config()
        val inAppMessage = InAppMessages.config()
        val user = user()
        val response = InAppMessages.layoutResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns response

        // when
        val actual = evaluateProcessor.layout(workspace, inAppMessage, user, DELIVER)

        // then
        expectThat(actual) isSameInstanceAs response
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageLayoutEvaluateRequest> {
                expectThat(it).isA<InAppMessageLayoutLocalEvaluateRequest>().and {
                    get { this.workspace } isSameInstanceAs workspace
                    get { entity } isSameInstanceAs inAppMessage
                    get { this.user } isSameInstanceAs user
                    get { scope } isEqualTo DELIVER
                    get { record } isEqualTo true
                    get { phase } isEqualTo EvaluationPhase.RUNTIME
                }
            })
        }
    }

    @Test
    fun `eligibility(Remote) - REMOTE eligibility 요청을 조립해 평가한다`() {
        // given
        val workspace = Workspaces.evaluation()
        val inAppMessage = InAppMessages.eligibilityRemoteResult()
        val user = user()
        val response = InAppMessages.eligibilityResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns response

        // when
        val actual = evaluateProcessor.eligibility(workspace, inAppMessage, user, TRIGGER, timestamp = 42)

        // then
        expectThat(actual) isSameInstanceAs response
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageEligibilityEvaluateRequest> {
                expectThat(it).isA<InAppMessageEligibilityRemoteEvaluateRequest>().and {
                    get { this.workspace } isSameInstanceAs workspace
                    get { entity } isSameInstanceAs inAppMessage
                    get { this.user } isSameInstanceAs user
                    get { scope } isEqualTo TRIGGER
                    get { platformType } isEqualTo PlatformType.ANDROID
                    get { timestamp } isEqualTo 42L
                    get { record } isEqualTo true
                }
            })
        }
    }

    @Test
    fun `eligibility(Remote) - record 값을 요청에 전달한다`() {
        // given
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns InAppMessages.eligibilityResponse()

        // when
        evaluateProcessor.eligibility(
            workspace = Workspaces.evaluation(),
            inAppMessage = InAppMessages.eligibilityRemoteResult(),
            user = user(),
            scope = DELIVER,
            timestamp = 42,
            record = false
        )

        // then
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageEligibilityEvaluateRequest> {
                expectThat(it.record) isEqualTo false
            })
        }
    }

    @Test
    fun `layout(Remote) - 결과의 layout을 entity로 REMOTE layout 요청을 조립해 평가한다`() {
        // given
        val workspace = Workspaces.evaluation()
        val inAppMessage = InAppMessages.eligibilityRemoteResult()
        val user = user()
        val response = InAppMessages.layoutResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns response

        // when
        val actual = evaluateProcessor.layout(workspace, inAppMessage, user, DELIVER)

        // then
        expectThat(actual) isSameInstanceAs response
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageLayoutEvaluateRequest> {
                expectThat(it).isA<InAppMessageLayoutRemoteEvaluateRequest>().and {
                    get { this.workspace } isSameInstanceAs workspace
                    get { entity } isSameInstanceAs inAppMessage.layout
                    get { this.user } isSameInstanceAs user
                    get { scope } isEqualTo DELIVER
                    get { record } isEqualTo true
                }
            })
        }
    }
}
