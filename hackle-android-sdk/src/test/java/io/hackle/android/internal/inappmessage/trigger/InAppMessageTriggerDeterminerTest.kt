package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluation
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluator
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
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
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class InAppMessageTriggerDeterminerTest {

    @MockK
    private lateinit var workspaceFetcher: WorkspaceFetcher

    @MockK
    private lateinit var eventMatcher: InAppMessageEventMatcher

    @MockK
    private lateinit var evaluator: InAppMessageEvaluator

    @InjectMockKs
    private lateinit var sut: InAppMessageTriggerDeterminer

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when event is not TrackEvent then return null`() {
        // given
        val event = mockk<UserEvent.Exposure>()

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `when workspace is null then return null`() {
        // given
        every { workspaceFetcher.fetch() } returns null
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `when inAppMessage is empty then return null`() {
        // given
        val workspace = mockk<Workspace>()
        every { workspace.inAppMessages } returns emptyList()
        every { workspaceFetcher.fetch() } returns workspace
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `when all inAppMessage do not matched then return null`() {
        // given
        determine(
            decision(isEventMatched = false, isEligible = false, reason = DecisionReason.IN_APP_MESSAGE_DRAFT),
            decision(isEventMatched = true, isEligible = false, reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET),
        )

        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `when inAppMessage matched then trigger that inAppMessage`() {
        // given
        determine(
            decision(isEventMatched = false, isEligible = false, reason = DecisionReason.IN_APP_MESSAGE_DRAFT),
            decision(isEventMatched = true, isEligible = false, reason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET),
            decision(isEventMatched = true, isEligible = true, reason = DecisionReason.IN_APP_MESSAGE_TARGET),
            decision(isEventMatched = false, isEligible = false, reason = DecisionReason.IN_APP_MESSAGE_DRAFT),
        )
        val event = UserEvents.track("test")

        // when
        val actual = sut.determine(event)

        // then
        expectThat(actual).isNotNull().and {
            get { this.evaluation } isEqualTo InAppMessageEvaluation(true, DecisionReason.IN_APP_MESSAGE_TARGET)
            get { this.event } isEqualTo event
        }
    }

    private fun determine(vararg decisions: Decision) {
        every { eventMatcher.matches(any(), any(), any()) } returnsMany decisions.map { it.isEventMatched }
        every { evaluator.evaluate(any(), any(), any(), any()) } returnsMany decisions.filter { it.isEventMatched }
            .map { it.evaluation }

        val iam = InAppMessages.create()
        val workspace = mockk<Workspace>()
        every { workspace.inAppMessages } returns decisions.map { iam }
        every { workspace.getInAppMessageOrNull(any()) } returns iam
        every { workspaceFetcher.fetch() } returns workspace
    }

    private fun decision(isEventMatched: Boolean, isEligible: Boolean, reason: DecisionReason): Decision {
        return Decision(isEventMatched, InAppMessageEvaluation(isEligible, reason))
    }

    private class Decision(
        val isEventMatched: Boolean,
        val evaluation: InAppMessageEvaluation,
    )
}