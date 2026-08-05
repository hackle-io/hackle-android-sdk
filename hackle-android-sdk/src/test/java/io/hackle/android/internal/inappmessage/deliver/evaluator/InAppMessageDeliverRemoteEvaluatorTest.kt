package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.Workspaces
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.DELIVER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateRequest
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.CompletableFuture

class InAppMessageDeliverRemoteEvaluatorTest {

    @MockK
    private lateinit var workspaceManager: WorkspaceEvaluationManager

    @MockK
    private lateinit var evaluateProcessor: EvaluateProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageDeliverRemoteEvaluator

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

        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation()

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
    fun `atDeliverTime false - 재평가 없이 캐시된 workspace로 평가한다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1, requestedAt = 42)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.eligibilityRemoteResult(
            key = 1,
            evaluateContext = InAppMessages.evaluateContext(atDeliverTime = false)
        )
        val cachedWorkspace = Workspaces.evaluation(inAppMessages = listOf(inAppMessage))
        every { workspaceManager.workspace(any()) } returns cachedWorkspace

        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns InAppMessages.layoutResponse()

        val eligibilityRequests = mutableListOf<InAppMessageEligibilityEvaluateRequest>()
        val eligibilityResponse = InAppMessages.eligibilityResponse()
        every { evaluateProcessor.inAppMessage(capture(eligibilityRequests)) } returns eligibilityResponse

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo true
            get { code }.isNull()
            get { evaluation }.isNotNull().and {
                get { eligibility } isSameInstanceAs eligibilityResponse
            }
        }
        verify(exactly = 0) { workspaceManager.evaluate(any(), any()) }
        expectThat(eligibilityRequests).hasSize(1)
        expectThat(eligibilityRequests[0]) {
            get { workspace } isSameInstanceAs cachedWorkspace
            get { scope } isEqualTo DELIVER
            get { timestamp } isEqualTo 42L
            get { record } isEqualTo true
        }
    }

    @Test
    fun `atDeliverTime true - 사전 체크가 ineligible이면 재평가 API를 호출하지 않고 캐시된 workspace로 기록 평가한다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1, requestedAt = 42)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.eligibilityRemoteResult(
            key = 1,
            evaluateContext = InAppMessages.evaluateContext(atDeliverTime = true)
        )
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation(inAppMessages = listOf(inAppMessage))

        val layoutRequests = mutableListOf<InAppMessageLayoutEvaluateRequest>()
        every { evaluateProcessor.inAppMessage(capture(layoutRequests)) } returns InAppMessages.layoutResponse()

        val ineligibleResponse = InAppMessages.eligibilityResponse(
            evaluation = InAppMessages.eligibilityEvaluation(
                result = InAppMessages.eligibilityResult(isEligible = false)
            )
        )
        val eligibilityRequests = mutableListOf<InAppMessageEligibilityEvaluateRequest>()
        every { evaluateProcessor.inAppMessage(capture(eligibilityRequests)) } returns ineligibleResponse

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.INELIGIBLE
            get { evaluation }.isNull()
        }
        verify(exactly = 0) { workspaceManager.evaluate(any(), any()) }
        expectThat(eligibilityRequests).hasSize(2)
        expectThat(eligibilityRequests[0].record).isEqualTo(false)
        expectThat(eligibilityRequests[1].record).isEqualTo(true)
        // ineligible로 노출되지 않더라도 A/B imbalance 방지를 위해 layout은 기록 평가 전에 record=true로 정확히 1회 평가되어야 한다
        expectThat(layoutRequests).hasSize(1)
        expectThat(layoutRequests[0].record).isEqualTo(true)
        verifyOrder {
            evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>())
            evaluateProcessor.inAppMessage(match<InAppMessageEligibilityEvaluateRequest> { it.record })
        }
    }

    @Test
    fun `atDeliverTime true - 사전 체크가 eligible이면 재평가 API를 호출하고 새 workspace로 평가한다`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1, requestedAt = 42)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.eligibilityRemoteResult(
            key = 1,
            evaluateContext = InAppMessages.evaluateContext(atDeliverTime = true)
        )
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation(inAppMessages = listOf(inAppMessage))

        val freshInAppMessage = InAppMessages.eligibilityRemoteResult(
            key = 1,
            evaluateContext = InAppMessages.evaluateContext(atDeliverTime = true)
        )
        val freshWorkspace = Workspaces.evaluation(inAppMessages = listOf(freshInAppMessage))
        every { workspaceManager.evaluate(any(), any()) } returns CompletableFuture.completedFuture(freshWorkspace)

        every { evaluateProcessor.inAppMessage(any<InAppMessageLayoutEvaluateRequest>()) } returns InAppMessages.layoutResponse()

        val eligibilityRequests = mutableListOf<InAppMessageEligibilityEvaluateRequest>()
        val eligibilityResponse = InAppMessages.eligibilityResponse()
        every { evaluateProcessor.inAppMessage(capture(eligibilityRequests)) } returns eligibilityResponse

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo true
            get { code }.isNull()
            get { evaluation }.isNotNull().and {
                get { eligibility } isSameInstanceAs eligibilityResponse
            }
        }
        verify(exactly = 1) { workspaceManager.evaluate(any(), eq(listOf(inAppMessage))) }
        expectThat(eligibilityRequests).hasSize(2)
        expectThat(eligibilityRequests[0]) {
            get { record } isEqualTo false
            get { entity } isSameInstanceAs inAppMessage
        }
        expectThat(eligibilityRequests[1]) {
            get { record } isEqualTo true
            get { workspace } isSameInstanceAs freshWorkspace
            get { entity } isSameInstanceAs freshInAppMessage
        }
    }

    @Test
    fun `atDeliverTime true - 재평가된 workspace에 인앱메시지가 없으면 IN_APP_MESSAGE_NOT_FOUND`() {
        // given
        val request = InAppMessages.deliverRequest(inAppMessageKey = 1, requestedAt = 42)
        val user = HackleUser.builder().identifier(IdentifierType.ID, "user").build()

        val inAppMessage = InAppMessages.eligibilityRemoteResult(
            key = 1,
            evaluateContext = InAppMessages.evaluateContext(atDeliverTime = true)
        )
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation(inAppMessages = listOf(inAppMessage))
        every {
            workspaceManager.evaluate(
                any(),
                any()
            )
        } returns CompletableFuture.completedFuture(Workspaces.evaluation())

        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns InAppMessages.eligibilityResponse()

        // when
        val actual = sut.evaluate(request, user).get()

        // then
        expectThat(actual) {
            get { isEligible } isEqualTo false
            get { code } isEqualTo Code.IN_APP_MESSAGE_NOT_FOUND
            get { evaluation }.isNull()
        }
    }
}
