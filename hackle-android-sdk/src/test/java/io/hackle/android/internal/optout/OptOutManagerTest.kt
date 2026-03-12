package io.hackle.android.internal.optout

import io.hackle.android.internal.event.DefaultEventProcessor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OptOutManagerTest {

    private val eventProcessor = mockk<DefaultEventProcessor>(relaxed = true)

    @Test
    fun `default optOut is false`() {
        val optOutState = OptOutState(false)
        val manager = OptOutManager(eventProcessor, optOutState)
        expectThat(manager.isOptOutTracking).isFalse()
    }

    @Test
    fun `config true makes optOut true`() {
        val optOutState = OptOutState(true)
        val manager = OptOutManager(eventProcessor, optOutState)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `setOptOutTracking true flushes events then blocks`() {
        val optOutState = OptOutState(false)
        val manager = OptOutManager(eventProcessor, optOutState)
        manager.setOptOutTracking(true)
        expectThat(manager.isOptOutTracking).isTrue()
        verify(exactly = 1) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking true flushes before state change`() {
        val optOutState = OptOutState(false)
        val manager = OptOutManager(eventProcessor, optOutState)
        every { eventProcessor.flush() } answers {
            // flush 호출 시점에 아직 optOut이 false여야 한다
            expectThat(manager.isOptOutTracking).isFalse()
        }
        manager.setOptOutTracking(true)
        verify(exactly = 1) { eventProcessor.flush() }
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `setOptOutTracking false restores optIn`() {
        val optOutState = OptOutState(true)
        val manager = OptOutManager(eventProcessor, optOutState)
        manager.setOptOutTracking(false)
        expectThat(manager.isOptOutTracking).isFalse()
        verify(exactly = 0) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking same value is no-op`() {
        val optOutState = OptOutState(false)
        val manager = OptOutManager(eventProcessor, optOutState)
        manager.setOptOutTracking(false)
        verify(exactly = 0) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking true does not flush if already optOut`() {
        val optOutState = OptOutState(true)
        val manager = OptOutManager(eventProcessor, optOutState)
        manager.setOptOutTracking(true)
        verify(exactly = 0) { eventProcessor.flush() }
    }
}
