package io.hackle.android.internal.optout

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.sdk.core.event.UserEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class OptOutUserEventFilterTest {

    private val optOutManager = mockk<OptOutManager>()
    private val filter = OptOutUserEventFilter(optOutManager)

    @Test
    fun `when optOut is true, returns BLOCK`() {
        every { optOutManager.isOptOutTracking } returns true
        val event = mockk<UserEvent>()
        expectThat(filter.check(event)).isEqualTo(UserEventFilter.Result.BLOCK)
    }

    @Test
    fun `when optOut is false, returns PASS`() {
        every { optOutManager.isOptOutTracking } returns false
        val event = mockk<UserEvent>()
        expectThat(filter.check(event)).isEqualTo(UserEventFilter.Result.PASS)
    }
}
