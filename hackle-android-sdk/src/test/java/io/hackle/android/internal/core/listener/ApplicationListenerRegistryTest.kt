package io.hackle.android.internal.core.listener

import io.hackle.android.internal.core.Ordered
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

internal class ApplicationListenerRegistryTest {

    @Test
    fun `addListener should add listener with default order`() {
        val registry = TestApplicationListenerRegistry()
        val listener = mockk<TestApplicationListener>()

        registry.addListener(listener)

        expectThat(registry.listeners).containsExactly(listener)
    }

    @Test
    fun `addListener should maintain order when adding listeners with different priorities`() {
        val registry = TestApplicationListenerRegistry()
        val highPriorityListener = mockk<TestApplicationListener>()
        val mediumPriorityListener = mockk<TestApplicationListener>()
        val lowPriorityListener = mockk<TestApplicationListener>()

        registry.addListener(mediumPriorityListener, Ordered.MEDIUM)
        registry.addListener(lowPriorityListener, Ordered.LOWEST)
        registry.addListener(highPriorityListener, Ordered.HIGHEST)

        expectThat(registry.listeners).containsExactly(
            highPriorityListener,
            mediumPriorityListener,
            lowPriorityListener
        )
    }

    @Test
    fun `addListener should handle same priority listeners`() {
        val registry = TestApplicationListenerRegistry()
        val listener1 = mockk<TestApplicationListener>()
        val listener2 = mockk<TestApplicationListener>()

        registry.addListener(listener1, Ordered.MEDIUM)
        registry.addListener(listener2, Ordered.MEDIUM)

        expectThat(registry.listeners).containsExactly(listener1, listener2)
    }

    @Test
    fun `listeners should be empty initially`() {
        val registry = TestApplicationListenerRegistry()

        expectThat(registry.listeners).isEmpty()
    }

    @Test
    fun `addListener should be thread safe`() {
        val registry = TestApplicationListenerRegistry()
        val listeners = mutableListOf<TestApplicationListener>()
        
        // Create 10 listeners
        repeat(10) {
            listeners.add(mockk<TestApplicationListener>())
        }

        // Add listeners from multiple threads
        val threads = listeners.mapIndexed { index, listener ->
            Thread {
                registry.addListener(listener, index)
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Should have all listeners
        assert(registry.listeners.size == 10)
        
        // Should be properly ordered (ascending by order value)
        val orders = registry.listeners.map { listener ->
            listeners.indexOf(listener)
        }
        expectThat(orders).containsExactly(orders.sorted())
    }

    private class TestApplicationListenerRegistry : ApplicationListenerRegistry<TestApplicationListener>()

    internal interface TestApplicationListener : ApplicationListener
}