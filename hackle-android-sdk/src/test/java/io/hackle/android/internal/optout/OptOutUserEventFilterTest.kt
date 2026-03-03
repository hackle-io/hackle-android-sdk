package io.hackle.android.internal.optout

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.sdk.core.event.UserEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OptOutUserEventFilterTest {

    private val optOutManager = mockk<OptOutManager>()
    private val filter = OptOutUserEventFilter(optOutManager)

    @Test
    fun `when optOut is true, returns BLOCK`() {
        every { optOutManager.isOptOut } returns true
        val event = mockk<UserEvent>()
        assertEquals(UserEventFilter.Result.BLOCK, filter.check(event))
    }

    @Test
    fun `when optOut is false, returns PASS`() {
        every { optOutManager.isOptOut } returns false
        val event = mockk<UserEvent>()
        assertEquals(UserEventFilter.Result.PASS, filter.check(event))
    }
}
