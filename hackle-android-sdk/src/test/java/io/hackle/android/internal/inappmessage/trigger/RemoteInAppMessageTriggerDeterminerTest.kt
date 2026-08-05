package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.workspace.evaluation.WorkspaceEvaluationManager
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.UserEvents
import io.hackle.android.support.Workspaces
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope.TRIGGER
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateRequest
import io.hackle.sdk.core.event.UserEvent
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
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class RemoteInAppMessageTriggerDeterminerTest {

    @MockK
    private lateinit var eventMatcher: InAppMessageEventMatcher

    @MockK
    private lateinit var workspaceManager: WorkspaceEvaluationManager

    @MockK
    private lateinit var evaluateProcessor: EvaluateProcessor

    @InjectMockKs
    private lateinit var sut: RemoteInAppMessageTriggerDeterminer

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `Track 이벤트가 아니면 null을 리턴한다`() {
        // given
        val event = mockk<UserEvent.Exposure>()

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `workspace가 없으면 null을 리턴한다`() {
        // given
        every { workspaceManager.workspace(any()) } returns null
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `inAppMessage가 없으면 null을 리턴한다`() {
        // given
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation()
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `매칭되고 eligible한 inAppMessage가 없으면 null을 리턴한다`() {
        // given
        val messages = listOf(
            InAppMessages.eligibilityRemoteResult(id = 1, key = 1),
            InAppMessages.eligibilityRemoteResult(id = 2, key = 2),
        )
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation(inAppMessages = messages)
        every { eventMatcher.matches(any(), any(), any()) } returnsMany listOf(false, true)
        every { evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>()) } returns InAppMessages.eligibilityResponse(
            evaluation = InAppMessages.eligibilityEvaluation(
                result = InAppMessages.eligibilityResult(
                    isEligible = false,
                    reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET
                )
            )
        )
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
        verify(exactly = 1) {
            evaluateProcessor.inAppMessage(any<InAppMessageEligibilityEvaluateRequest>())
        }
    }

    @Test
    fun `매칭되고 eligible한 첫번째 inAppMessage를 트리거한다`() {
        // given
        val messages = listOf(
            InAppMessages.eligibilityRemoteResult(id = 1, key = 1),
            InAppMessages.eligibilityRemoteResult(id = 2, key = 2),
            InAppMessages.eligibilityRemoteResult(id = 3, key = 3),
            InAppMessages.eligibilityRemoteResult(id = 4, key = 4),
        )
        every { workspaceManager.workspace(any()) } returns Workspaces.evaluation(inAppMessages = messages)
        every { eventMatcher.matches(any(), any(), any()) } returnsMany listOf(false, true, true)

        val requests = mutableListOf<InAppMessageEligibilityEvaluateRequest>()
        every { evaluateProcessor.inAppMessage(capture(requests)) } returnsMany listOf(
            InAppMessages.eligibilityResponse(
                evaluation = InAppMessages.eligibilityEvaluation(
                    result = InAppMessages.eligibilityResult(
                        isEligible = false,
                        reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET
                    )
                )
            ),
            InAppMessages.eligibilityResponse(
                evaluation = InAppMessages.eligibilityEvaluation(
                    result = InAppMessages.eligibilityResult(
                        isEligible = true,
                        reason = DecisionReason.IN_APP_MESSAGE_TARGET
                    )
                )
            ),
        )
        val event = UserEvents.track("test", timestamp = 42)

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNotNull().and {
            get { inAppMessage } isSameInstanceAs messages[2]
            get { reason } isEqualTo DecisionReason.IN_APP_MESSAGE_TARGET
            get { this.event } isSameInstanceAs event
        }
        expectThat(requests[1]) {
            get { entity } isSameInstanceAs messages[2]
            get { scope } isEqualTo TRIGGER
            get { timestamp } isEqualTo 42L
        }
    }
}
