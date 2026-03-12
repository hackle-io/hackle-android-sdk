package io.hackle.android.internal.optout

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OptOutStateTest {

    @Test
    fun `default false`() {
        val state = OptOutState(false)
        expectThat(state.isOptOutTracking).isFalse()
    }

    @Test
    fun `default true`() {
        val state = OptOutState(true)
        expectThat(state.isOptOutTracking).isTrue()
    }

    @Test
    fun `set to true`() {
        val state = OptOutState(false)
        state.isOptOutTracking = true
        expectThat(state.isOptOutTracking).isTrue()
    }

    @Test
    fun `set to false`() {
        val state = OptOutState(true)
        state.isOptOutTracking = false
        expectThat(state.isOptOutTracking).isFalse()
    }
}
