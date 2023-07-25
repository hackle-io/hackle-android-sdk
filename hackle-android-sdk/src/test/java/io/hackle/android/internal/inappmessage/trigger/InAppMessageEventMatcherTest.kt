package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.workspace.Workspace
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


class InAppMessageEventMatcherTest {

    @MockK
    private lateinit var ruleDeterminer: InAppMessageEventTriggerDeterminer

    @MockK
    private lateinit var frequencyCapDeterminer: InAppMessageEventTriggerDeterminer

    @InjectMockKs
    private lateinit var sut: InAppMessageEventMatcher

    private lateinit var workspace: Workspace

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        workspace = mockk()
    }

    @Test
    fun `when event is not TrackEvent when returns false`() {
        // given
        val exposureEvent = mockk<UserEvent.Exposure>()
        val inAppMessage = InAppMessages.create()

        // when
        val actual = sut.matches(workspace, inAppMessage, exposureEvent)

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `not trigger target - rule`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create()
        every { ruleDeterminer.isTriggerTarget(any(), any(), any()) } returns false

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `not trigger target - frequency cap`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create()
        every { ruleDeterminer.isTriggerTarget(any(), any(), any()) } returns true
        every { frequencyCapDeterminer.isTriggerTarget(any(), any(), any()) } returns false

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
    }
    
    @Test
    fun `trigger target`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create()
        every { ruleDeterminer.isTriggerTarget(any(), any(), any()) } returns true
        every { frequencyCapDeterminer.isTriggerTarget(any(), any(), any()) } returns true

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
    }
}
