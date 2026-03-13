package io.hackle.android.internal.optout

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OptOutManagerTest {

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
    fun `setOptOutTracking true changes state`() {
        val manager = OptOutManager(false)
        manager.setOptOutTracking(true)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `setOptOutTracking false changes state`() {
        val manager = OptOutManager(true)
        manager.setOptOutTracking(false)
        expectThat(manager.isOptOutTracking).isFalse()
    }

    @Test
    fun `setOptOutTracking same value is no-op`() {
        val manager = OptOutManager(false)
        manager.setOptOutTracking(false)
        expectThat(manager.isOptOutTracking).isFalse()
    }

    @Test
    fun `setOptOutTracking true does not change if already optOut`() {
        val manager = OptOutManager(true)
        manager.setOptOutTracking(true)
        expectThat(manager.isOptOutTracking).isTrue()
    }
}
