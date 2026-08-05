package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.workspace.config.WorkspaceConfigManager
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.DELIVER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateRequest
import io.hackle.sdk.core.model.PlatformType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class InAppMessageDeliverLocalEvaluatorTest {

    @MockK
    private lateinit var workspaceManager: WorkspaceConfigManager

    @MockK
    private lateinit var evaluateProcessor: EvaluateProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageDeliverLocalEvaluator

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `WORKSPACE_NOT_FOUND - workspace가 없으면 평가하지 않는다`() {
        // given
        val request = InAppMessages.deliverRequest()
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        every { workspaceManager.workspace(any()) } returns null

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.WORKSPACE_NOT_FOUND
            get { evaluation }.isNull()
        }
        verify { evaluateProcessor wasNot Called }
    }

    @Test
    fun `IN_APP_MESSAGE_NOT_FOUND - 인앱메시지가 없으면 평가하지 않는다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        every { workspaceManager.workspace(any()) } returns Workspaces.config()

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.IN_APP_MESSAGE_NOT_FOUND
            get { evaluation }.isNull()
        }
        verify { evaluateProcessor wasNot Called }
    }

    @Test
    fun `eligible - DELIVER scope로 layout과 eligibility를 평가한다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1, requestedAt = 42)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.config(key = 1)
        every { workspaceManager.workspace(any()) } returns Workspaces.config(inAppMessages = listOf(inAppMessage))

        val layoutResponse = InAppMessages.layoutResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns layoutResponse

        val eligibilityResponse = InAppMessages.eligibilityResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns eligibilityResponse

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo true
            get { code }.isNull()
            get { evaluation }.isNotNull().and {
                get { eligibility } isSameInstanceAs eligibilityResponse
                get { layout } isSameInstanceAs layoutResponse
            }
        }
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageLayoutEvaluateRequest> {
                expectThat(it) {
                    get { entity } isSameInstanceAs inAppMessage
                    get { scope } isEqualTo DELIVER
                    get { record } isEqualTo true
                }
            })
        }
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(withArg<InAppMessageEligibilityEvaluateRequest> {
                expectThat(it) {
                    get { entity } isSameInstanceAs inAppMessage
                    get { scope } isEqualTo DELIVER
                    get { platformType } isEqualTo PlatformType.ANDROID
                    get { timestamp } isEqualTo 42L
                    get { record } isEqualTo true
                }
            })
        }
    }

    @Test
    fun `INELIGIBLE - ineligible이어도 layout은 record하고 evaluation 없이 응답한다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.config(key = 1)
        every { workspaceManager.workspace(any()) } returns Workspaces.config(inAppMessages = listOf(inAppMessage))

        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns InAppMessages.layoutResponse()
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns InAppMessages.eligibilityResponse(
            evaluation = InAppMessages.eligibilityEvaluation(
                result = InAppMessages.eligibilityResult(isEligible = false)
            )
        )

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.INELIGIBLE
            get { evaluation }.isNull()
        }
        // ineligible로 노출되지 않더라도 A/B imbalance 방지를 위해 layout은 eligibility 전에 record=true로 평가되어야 한다
        verifyOrder {
            evaluateProcessor.inAppMessage(withArg<InAppMessageLayoutEvaluateRequest> {
                expectThat(it.record).isEqualTo(true)
            })
            evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>())
        }
    }
}
