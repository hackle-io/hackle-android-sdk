package io.hackle.android.internal.inappmessage.trigger

import io.hackle.android.internal.event.UserEvents
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class InAppMessageTriggerProcessorTest {

    @MockK
    private lateinit var determiner: InAppMessageTriggerDeterminer

    @RelaxedMockK
    private lateinit var handler: InAppMessageTriggerHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageTriggerProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when trigger not determined then do not handle`() {
        // given
        every { determiner.determine(any()) } returns null
        val event = UserEvents.track("test")

        // when
        sut.process(event)

        // then
        verify(exactly = 0) {
            handler.handle(any())
        }
    }

    @Test
    fun `when trigger determined then handle trigger`() {
        // given
        val trigger = mockk<InAppMessageTrigger>()
        every { determiner.determine(any()) } returns trigger

        val event = UserEvents.track("test")

        // when
        sut.process(event)

        // then
        verify(exactly = 1) {
            handler.handle(trigger)
        }
    }

    @Test
    fun `when exception occurs during handle trigger then ignore`() {
        val trigger = mockk<InAppMessageTrigger>()
        every { determiner.determine(any()) } returns trigger
        every { handler.handle(any()) } throws IllegalArgumentException("fail")

        val event = UserEvents.track("test")
        sut.process(event)
    }
}
