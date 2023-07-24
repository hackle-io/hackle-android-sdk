package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.support.InAppMessages
import io.hackle.android.support.Targets
import io.hackle.android.support.Targets.condition
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.model.InAppMessage
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InAppMessageEventTriggerRuleDeterminerTest {

    @MockK
    private lateinit var targetMatcher: TargetMatcher

    @InjectMockKs
    private lateinit var sut: InAppMessageEventTriggerRuleDeterminer

    private lateinit var workspace: Workspace

    @Before
    fun before() {
        MockKAnnotations.init(this)
        workspace = mockk()
    }

    @Test
    fun `when trigger rule is empty then returns false`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create(eventTrigger = InAppMessages.eventTrigger(rules = listOf()))

        // when
        val actual = sut.isTriggerTarget(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `when all trigger rules do no match then returns false`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create(
            eventTrigger = InAppMessages.eventTrigger(
                rules = listOf(
                    InAppMessage.EventTrigger.Rule("not_match", listOf()),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition()))),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition(), condition()))),
                )
            )
        )
        every { targetMatcher.anyMatches(any(), any(), any()) } returnsMany listOf(false, false)

        // when
        val actual = sut.isTriggerTarget(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
        verify(exactly = 2) {
            targetMatcher.anyMatches(any(), any(), any())
        }
    }

    @Test
    fun `when trigger rule matched then returns true`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create(
            eventTrigger = InAppMessages.eventTrigger(
                rules = listOf(
                    InAppMessage.EventTrigger.Rule("not_match", listOf()),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition()))),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition()))),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition()))),
                    InAppMessage.EventTrigger.Rule("test", listOf(Targets.create(condition()))),
                )
            )
        )
        every { targetMatcher.anyMatches(any(), any(), any()) } returnsMany listOf(false, false, true, false)

        // when
        val actual = sut.isTriggerTarget(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
        verify(exactly = 3) {
            targetMatcher.anyMatches(any(), any(), any())
        }
    }
}
