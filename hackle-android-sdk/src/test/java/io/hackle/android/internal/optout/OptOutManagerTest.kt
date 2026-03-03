package io.hackle.android.internal.optout

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.event.DefaultEventProcessor
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OptOutManagerTest {

    private val repository = MapKeyValueRepository()
    private val eventProcessor = mockk<DefaultEventProcessor>(relaxed = true)

    @Test
    fun `default optOut is false`() {
        val manager = OptOutManager(repository, eventProcessor, false)
        expectThat(manager.isOptOutTracking).isFalse()
    }

    @Test
    fun `config true makes optOut true`() {
        val manager = OptOutManager(repository, eventProcessor, true)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `config true saves to local`() {
        OptOutManager(repository, eventProcessor, true)
        expectThat(repository.getString("opt_out_tracking")?.toBoolean() == true).isTrue()
    }

    @Test
    fun `saved true makes optOut true even if config is false`() {
        repository.putString("opt_out_tracking", "true")
        val manager = OptOutManager(repository, eventProcessor, false)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `config true and saved true makes optOut true`() {
        repository.putString("opt_out_tracking", "true")
        val manager = OptOutManager(repository, eventProcessor, true)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `config true and saved false makes optOut true`() {
        repository.putString("opt_out_tracking", "false")
        val manager = OptOutManager(repository, eventProcessor, true)
        expectThat(manager.isOptOutTracking).isTrue()
    }

    @Test
    fun `setOptOutTracking true flushes events then blocks`() {
        val manager = OptOutManager(repository, eventProcessor, false)
        manager.setOptOutTracking(true)
        expectThat(manager.isOptOutTracking).isTrue()
        verify(exactly = 1) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking true saves to local`() {
        val manager = OptOutManager(repository, eventProcessor, false)
        manager.setOptOutTracking(true)
        expectThat(repository.getString("opt_out_tracking")?.toBoolean() == true).isTrue()
    }

    @Test
    fun `setOptOutTracking false restores optIn`() {
        val manager = OptOutManager(repository, eventProcessor, true)
        manager.setOptOutTracking(false)
        expectThat(manager.isOptOutTracking).isFalse()
        verify(exactly = 0) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking false saves to local`() {
        val manager = OptOutManager(repository, eventProcessor, true)
        manager.setOptOutTracking(false)
        expectThat(repository.getString("opt_out_tracking")?.toBoolean() == false).isTrue()
    }

    @Test
    fun `setOptOutTracking same value is no-op`() {
        val manager = OptOutManager(repository, eventProcessor, false)
        manager.setOptOutTracking(false)
        verify(exactly = 0) { eventProcessor.flush() }
    }

    @Test
    fun `setOptOutTracking true does not flush if already optOut`() {
        val manager = OptOutManager(repository, eventProcessor, true)
        manager.setOptOutTracking(true)
        verify(exactly = 0) { eventProcessor.flush() }
    }
}
