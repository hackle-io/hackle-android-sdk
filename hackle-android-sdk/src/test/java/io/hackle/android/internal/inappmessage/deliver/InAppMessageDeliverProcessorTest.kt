package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateProcessor
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateType
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageLayoutResolver
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.TRIGGERED
import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.activity.lifecycle.ActivityState
import io.hackle.android.internal.session.SessionUserDecorator
import io.hackle.android.internal.user.UserManager
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var sessionUserDecorator: SessionUserDecorator

    @MockK
    private lateinit var identifierChecker: InAppMessageIdentifierChecker

    @MockK
    private lateinit var layoutResolver: InAppMessageLayoutResolver

    @MockK
    private lateinit var evaluateProcessor: InAppMessageEvaluateProcessor

    @MockK
    private lateinit var presentProcessor: InAppMessagePresentProcessor

    @InjectMockKs
    private lateinit var sut: InAppMessageDeliverProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { sessionUserDecorator.decorate(any()) } answers {
            firstArg<HackleUser>().toBuilder().identifier(IdentifierType.SESSION, "session").build()
        }
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

        every { userManager.resolve(any(), any()) } returns HackleUser.builder().build()
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
            evaluateContext = InAppMessage.EvaluateContext(atDeliverTime = true)
        )
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val layoutEvaluation = InAppMessages.layoutEvaluation()
        every { layoutResolver.resolve(any(), any(), any()) } returns layoutEvaluation

        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(
            isEligible = false
        )
        every { evaluateProcessor.process(InAppMessageEvaluateType.DELIVER, any()) } returns eligibilityEvaluation

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
    }

    @Test
    fun `INELIGIBLE when timetable check fails at deliver time`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE

        val workspace = mockk<Workspace>()
        every { workspaceFetcher.fetch() } returns workspace

        val inAppMessage = InAppMessages.create(
            evaluateContext = InAppMessage.EvaluateContext(atDeliverTime = true)
        )
        every { workspace.getInAppMessageOrNull(any()) } returns inAppMessage

        every { userManager.resolve(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val layoutEvaluation = InAppMessages.layoutEvaluation()
        every { layoutResolver.resolve(any(), any(), any()) } returns layoutEvaluation

        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(
            isEligible = false,
            reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TIMETABLE
        )
        every { evaluateProcessor.process(InAppMessageEvaluateType.DELIVER, any()) } returns eligibilityEvaluation

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
    }


    @Test
    fun `PRESENT`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(reason = DecisionReason.IN_APP_MESSAGE_TARGET).toRequest(TRIGGERED, 42)
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

        every { userManager.resolve(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val layoutEvaluation = InAppMessages.layoutEvaluation()
        every { layoutResolver.resolve(any(), any(), any()) } returns layoutEvaluation

        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(
            isEligible = true
        )
        every { evaluateProcessor.process(InAppMessageEvaluateType.DELIVER, any()) } returns eligibilityEvaluation

        val presentResponse = mockk<InAppMessagePresentResponse>()
        every { presentProcessor.process(any()) } returns presentResponse

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.PRESENT, presentResponse)
        verify {
            presentProcessor.process(withArg {
                expectThat(it) {
                    get { user.sessionId } isEqualTo "session"
                }
            })
        }
    }

    @Test
    fun `EXCEPTION`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(reason = DecisionReason.IN_APP_MESSAGE_TARGET).toRequest(TRIGGERED, 42)
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

        every { userManager.resolve(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val layoutEvaluation = InAppMessages.layoutEvaluation()
        every { layoutResolver.resolve(any(), any(), any()) } returns layoutEvaluation

        val eligibilityEvaluation = InAppMessages.eligibilityEvaluation(
            isEligible = true
        )
        every { evaluateProcessor.process(InAppMessageEvaluateType.DELIVER, any()) } returns eligibilityEvaluation

        val presentResponse = mockk<InAppMessagePresentResponse>()
        every { presentProcessor.process(any()) } throws IllegalArgumentException("fail")

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.EXCEPTION)
    }
}
