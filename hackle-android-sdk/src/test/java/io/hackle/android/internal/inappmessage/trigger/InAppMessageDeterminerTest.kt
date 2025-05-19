package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.decision.InAppMessageDecision
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.InAppMessage
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
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class InAppMessageDeterminerTest {

    @MockK
    private lateinit var workspaceFetcher: WorkspaceFetcher

    @MockK
    private lateinit var inAppMessageEventMatcher: InAppMessageEventMatcher

    @MockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: InAppMessageDeterminer

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `Workspace 가 없으면 null 리턴`() {
        // given
        every { workspaceFetcher.fetch() } returns null
        val event = mockk<UserEvent.Track>()

        // when
        val actual = sut.determineOrNull(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `InAppMessage 가 없으면 null 리턴`() {
        // given
        val workspace = mockk<Workspace>(relaxed = true)
        every { workspaceFetcher.fetch() } returns workspace
        val event = mockk<UserEvent.Track>()

        // when
        val actual = sut.determineOrNull(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `일치하는 InAppMessage가 하나도 없으면 null 리턴`() {
        // given
        determine(
            decision(true),
            decision(true, null),
            decision(true, InAppMessages.create(), null),
        )
        val event = UserEvents.track("test")

        // when
        val actual = sut.determineOrNull(event)

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `일치하는 InAppMessage 가 있는 경우`() {
        // given
        val message = InAppMessages.message()
        val inAppMessage =
            InAppMessages.create(id = 42, messageContext = InAppMessages.messageContext(messages = listOf(message)))

        determine(
            decision(true),
            decision(true, null),
            decision(true, InAppMessages.create(), null),
            decision(true, inAppMessage, message, DecisionReason.IN_APP_MESSAGE_TARGET, mapOf("a" to 42)),
            decision(false),
        )
        val event = UserEvents.track("test", insertId = "insert")

        // when
        val actual = sut.determineOrNull(event)

        // then
        expectThat(actual).isNotNull().and {
            get { this.inAppMessage } isSameInstanceAs inAppMessage
            get { this.message } isSameInstanceAs message
            get { this.properties } isEqualTo mapOf(
                "decision_reason" to "IN_APP_MESSAGE_TARGET",
                "trigger_event_insert_id" to "insert",
                "a" to 42
            )
        }
        verify(exactly = 4) {
            inAppMessageEventMatcher.matches(any(), any(), any())
        }
        verify(exactly = 4) {
            core.inAppMessage(any(), any())
        }
    }

    private fun determine(vararg decisions: Decision) {
        every { inAppMessageEventMatcher.matches(any(), any(), any()) } returnsMany decisions.map { it.isEventMatch }
        every { core.inAppMessage(any(), any()) } returnsMany decisions.map { it.decision }

        val iam = InAppMessages.create()
        val workspace = mockk<Workspace>()
        every { workspace.inAppMessages } returns decisions.map { iam }
        every { workspace.getInAppMessageOrNull(any()) } returns iam
        every { workspaceFetcher.fetch() } returns workspace
    }

    private fun decision(
        isMatch: Boolean,
        inAppMessage: InAppMessage? = null,
        message: InAppMessage.Message? = null,
        reason: DecisionReason = DecisionReason.NOT_IN_IN_APP_MESSAGE_TARGET,
        properties: Map<String, Any> = emptyMap()
    ): Decision {
        return Decision(isMatch, InAppMessageDecision.of(reason, inAppMessage, message, properties))
    }

    private class Decision(
        val isEventMatch: Boolean,
        val decision: InAppMessageDecision
    )
}
