package io.hackle.android.internal.optout

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OptOutManagerTest {

    private val listener = mockk<OptOutListener>(relaxed = true)

    @Test
    fun `default optOut is false`() {
        val manager = OptOutManager(false)
        expectThat(manager.isOptOutTracking).isFalse()
    }

    @Test
    fun `config true makes optOut true`() {
        val manager = OptOutManager(true)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `setOptOutTracking true changes state and notifies listener`() {
        val manager = OptOutManager(false)
        manager.addListener(listener)
        manager.setOptOutTracking(true)
        expectThat(manager.isOptOutTracking).isTrue()
        verify(exactly = 1) { listener.onOptOutChanged(true) }
    }

    @Test
    fun `setOptOutTracking false changes state and notifies listener`() {
        val manager = OptOutManager(true)
        manager.addListener(listener)
        manager.setOptOutTracking(false)
        expectThat(manager.isOptOutTracking).isFalse()
        verify(exactly = 1) { listener.onOptOutChanged(false) }
    }

    @Test
    fun `setOptOutTracking same value is no-op`() {
        val manager = OptOutManager(false)
        manager.addListener(listener)
        manager.setOptOutTracking(false)
        verify(exactly = 0) { listener.onOptOutChanged(any()) }
    }

    @Test
    fun `setOptOutTracking true does not notify if already optOut`() {
        val manager = OptOutManager(true)
        manager.addListener(listener)
        manager.setOptOutTracking(true)
        verify(exactly = 0) { listener.onOptOutChanged(any()) }
    }

    @Test
    fun `state changes before listener notification`() {
        val manager = OptOutManager(false)
        val verifyingListener = object : OptOutListener {
            var stateAtNotification: Boolean = false
            override fun onOptOutChanged(current: Boolean) {
                stateAtNotification = manager.isOptOutTracking
            }
        }
        manager.addListener(verifyingListener)
        manager.setOptOutTracking(true)
        expectThat(verifyingListener.stateAtNotification).isTrue()
    }
}
