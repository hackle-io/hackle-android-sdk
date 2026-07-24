package io.hackle.android.internal.inappmessage.deliver

import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.activity.lifecycle.ActivityState
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverResponse.Code
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluateResponse
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluation
import io.hackle.android.internal.inappmessage.deliver.evaluator.InAppMessageDeliverEvaluator
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleType.TRIGGERED
import io.hackle.android.internal.session.SessionUserDecorator
import io.hackle.android.internal.user.UserManager
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.CompletableFuture

class InAppMessageDeliverProcessorTest {

    @MockK
    private lateinit var activityProvider: ActivityProvider

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var sessionUserDecorator: SessionUserDecorator

    @MockK
    private lateinit var identifierChecker: InAppMessageIdentifierChecker

    @MockK
    private lateinit var evaluator: InAppMessageDeliverEvaluator

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
    fun `ACTIVITY_INACTIVE - Activity가 ACTIVE 상태가 아니면 present하지 않는다`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.INACTIVE

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.ACTIVITY_INACTIVE)
        verify { presentProcessor wasNot Called }
    }

    @Test
    fun `IDENTIFIER_CHANGED - 스케줄 시점과 유저 식별자가 달라졌으면 present하지 않는다`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { userManager.currentUser } returns User.builder().build()
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns true

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.IDENTIFIER_CHANGED)
        verify { presentProcessor wasNot Called }
    }

    @Test
    fun `INELIGIBLE - 평가 결과가 ineligible이면 present하지 않는다`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { userManager.currentUser } returns User.builder().build()
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(
                evaluation = InAppMessages.eligibilityEvaluation(
                    result = InAppMessages.eligibilityResult(
                        isEligible = false,
                        reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET
                    )
                )
            ),
            layout = InAppMessages.layoutResponse(),
        )
        every { evaluator.evaluate(any(), any()) } returns CompletableFuture.completedFuture(
            InAppMessageDeliverEvaluateResponse.of(evaluation)
        )

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.INELIGIBLE)
        verify { presentProcessor wasNot Called }
    }

    @Test
    fun `INELIGIBLE - evaluator가 반환한 code를 그대로 사용한다`() {
        // given
        val request = InAppMessageDeliverRequest.of(InAppMessages.schedule().toRequest(TRIGGERED, 42))

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { userManager.currentUser } returns User.builder().build()
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false
        every { evaluator.evaluate(any(), any()) } returns CompletableFuture.completedFuture(
            InAppMessageDeliverEvaluateResponse.ineligible(Code.WORKSPACE_NOT_FOUND)
        )

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.WORKSPACE_NOT_FOUND)
        verify { presentProcessor wasNot Called }
    }

    @Test
    fun `DELIVER - 평가 결과가 eligible이면 세션 데코레이트된 유저로 present 한다`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(reason = DecisionReason.IN_APP_MESSAGE_TARGET).toRequest(TRIGGERED, 42)
        )

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { userManager.currentUser } returns User.builder().build()
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(),
            layout = InAppMessages.layoutResponse(),
        )
        every { evaluator.evaluate(any(), any()) } returns CompletableFuture.completedFuture(
            InAppMessageDeliverEvaluateResponse.of(evaluation)
        )

        val presentResponse = mockk<InAppMessagePresentResponse>()
        every { presentProcessor.process(any()) } returns CompletableFuture.completedFuture(presentResponse)

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.DELIVER, presentResponse)
        verify {
            presentProcessor.process(withArg {
                expectThat(it) {
                    get { user.sessionId } isEqualTo "session"
                }
            })
        }
    }

    @Test
    fun `EXCEPTION - 처리 중 예외가 발생하면 EXCEPTION 응답으로 복구한다`() {
        // given
        val request = InAppMessageDeliverRequest.of(
            InAppMessages.schedule(reason = DecisionReason.IN_APP_MESSAGE_TARGET).toRequest(TRIGGERED, 42)
        )

        every { activityProvider.currentState } returns ActivityState.ACTIVE
        every { userManager.currentUser } returns User.builder().build()
        every { userManager.hackleUser(any(), any()) } returns HackleUser.builder().build()
        every { identifierChecker.isIdentifierChanged(any(), any()) } returns false

        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(),
            layout = InAppMessages.layoutResponse(),
        )
        every { evaluator.evaluate(any(), any()) } returns CompletableFuture.completedFuture(
            InAppMessageDeliverEvaluateResponse.of(evaluation)
        )

        every { presentProcessor.process(any()) } throws IllegalArgumentException("fail")

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isEqualTo InAppMessageDeliverResponse.of(request, Code.EXCEPTION)
    }
}
