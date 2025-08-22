package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluator
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.TRIGGERED
import io.hackle.android.internal.inappmessage.trigger.InAppMessageIdentifierChecker
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.ActivityState
import io.hackle.android.internal.user.UserManager
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageDeliverProcessorTest {

    @MockK
    private lateinit var activityProvider: ActivityProvider

    @MockK
    private lateinit var workspaceFetcher: WorkspaceFetcher

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var identifierChecker: InAppMessageIdentifierChecker

    @MockK
    private lateinit var evaluator: InAppMessageEvaluator

    @MockK
    private lateinit var presentProcessor: InAppMessagePresentProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageDeliverProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `ACTIVITY_INACTIVE`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.INACTIVE

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.ACTIVITY_INACTIVE)
    }

    @Test
    fun `WORKSPACE_NOT_FOUND`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { workspaceFetcher.fetch() } returns null

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.WORKSPACE_NOT_FOUND)
    }

    @Test
    fun `IN_APP_MESSAGE_NOT_FOUND`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        every { workspace.getInAppMessageOrNull(any()) } returns null

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.IN_APP_MESSAGE_NOT_FOUND)
    }

    @Test
    fun `IDENTIFIER_CHANGED`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        val inAppMessage = InAppMessages.create()
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns true

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.IDENTIFIER_CHANGED)
    }

    @Test
    fun `INELIGIBLE`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        val inAppMessage = InAppMessages.create(
            evaluateContext = InAppMessage.EvaluateContext(
                atDeliverTime = true
            )
        )
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        every {
            evaluator.evaluate(any(), any(), any(), any())
        } returns InAppMessageEvaluation(false, DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET)

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
    }


    @Test
    fun `PRESENT`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(
                evaluation = InAppMessageEvaluation(true, DecisionReason.IN_APP_MESSAGE_TARGET)
            ).toRequest(TRIGGERED, 42)
        )

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        val inAppMessage = InAppMessages.create(
            evaluateContext = InAppMessage.EvaluateContext(
                atDeliverTime = false
            )
        )
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        every {
            evaluator.evaluate(any(), any(), any(), any())
        } returns InAppMessageEvaluation(false, DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET)

        val presentResponse = mockk<InAppMessagePresentResponse>()
        every { presentProcessor.process(any()) } returns presentResponse

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.PRESENT, presentResponse)
    }

    @Test
    fun `EXCEPTION`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(
                evaluation = InAppMessageEvaluation(true, DecisionReason.IN_APP_MESSAGE_TARGET)
            ).toRequest(TRIGGERED, 42)
        )

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        val inAppMessage = InAppMessages.create(
            evaluateContext = InAppMessage.EvaluateContext(
                atDeliverTime = false
            )
        )
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        every {
            evaluator.evaluate(any(), any(), any(), any())
        } returns InAppMessageEvaluation(false, DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET)

        val presentResponse = mockk<InAppMessagePresentResponse>()
        every { presentProcessor.process(any()) } throws IllegalArgumentException("fail")

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.EXCEPTION)
    }
}
