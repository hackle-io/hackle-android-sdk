package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.support.InAppMessages
import io.hackle.android.support.Targets
import io.hackle.android.support.Targets.condition
import io.hackle.android.support.UserEvents
import io.hackle.sdk.core.evaluation.match.TargetMatcher
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

class InAppMessageEventMatcherTest {

    @MockK
    private lateinit var targetMatcher: TargetMatcher

    @InjectMockKs
    private lateinit var sut: InAppMessageEventMatcher

    @Before
    fun before() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `when trigger rule is empty then returns false`() {
        // given
        val workspace = mockk<Workspace>()
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.config(eventTrigger = InAppMessages.eventTrigger(rules = listOf()))

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `when all trigger rules do no match then returns false`() {
        // given
        val workspace = mockk<Workspace>()
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.config(
            eventTrigger = InAppMessages.eventTrigger(
                rules = listOf(
                    InAppMessages.eventTriggerRule("not_match"),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition()))),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition(), condition()))),
                )
            )
        )
        every { targetMatcher.anyMatches(any(), any(), any()) } returnsMany listOf(false, false)

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
        verify(exactly = 2) {
            targetMatcher.anyMatches(any(), any(), any())
        }
    }

    @Test
    fun `when trigger rule matched then returns true`() {
        // given
        val workspace = mockk<Workspace>()
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.config(
            eventTrigger = InAppMessages.eventTrigger(
                rules = listOf(
                    InAppMessages.eventTriggerRule("not_match"),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition()))),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition()))),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition()))),
                    InAppMessages.eventTriggerRule("test", listOf(Targets.create(condition()))),
                )
            )
        )
        every { targetMatcher.anyMatches(any(), any(), any()) } returnsMany listOf(false, false, true, false)

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
        verify(exactly = 3) {
            targetMatcher.anyMatches(any(), any(), any())
        }
    }
}
