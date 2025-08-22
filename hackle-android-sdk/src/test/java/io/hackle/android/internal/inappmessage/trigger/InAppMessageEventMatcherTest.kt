package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.hackle.android.support.InAppMessages
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
    private lateinit var ruleMatcher: InAppMessageEventTriggerRuleMatcher

    @InjectMockKs
    private lateinit var sut: InAppMessageEventMatcher

    private lateinit var workspace: Workspace

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        workspace = mockk()
    }

    @Test
    fun `not match`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create()
        every { ruleMatcher.matches(any(), any(), any()) } returns false

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isFalse()
    }

    @Test
    fun `match`() {
        // given
        val event = UserEvents.track("test")
        val inAppMessage = InAppMessages.create()
        every { ruleMatcher.matches(any(), any(), any()) } returns true

        // when
        val actual = sut.matches(workspace, inAppMessage, event)

        // then
        expectThat(actual).isTrue()
    }
}
