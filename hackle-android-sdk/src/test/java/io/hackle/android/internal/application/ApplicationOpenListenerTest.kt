package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class ApplicationOpenListenerTest {

    @Test
    fun `ApplicationOpenListener should extend ApplicationListener`() {
        val listener = mockk<ApplicationOpenListener>()
        expectThat(listener).isA<ApplicationListener>()
    }

    @Test
    fun `onApplicationOpened should be called with timestamp`() {
        // given
        val listener = mockk<ApplicationOpenListener>(relaxed = true)
        val timestamp = 1234567890L

        // when
        listener.onApplicationOpened(timestamp)

        // then
        verify { listener.onApplicationOpened(timestamp) }
    }

    @Test
    fun `ApplicationOpenListener implementation should work correctly`() {
        // given
        var capturedTimestamp: Long? = null
        val listener = object : ApplicationOpenListener {
            override fun onApplicationOpened(timestamp: Long) {
                capturedTimestamp = timestamp
            }
        }
        val timestamp = 1234567890L

        // when
        listener.onApplicationOpened(timestamp)

        // then
        expectThat(capturedTimestamp).isEqualTo(timestamp)
    }
}