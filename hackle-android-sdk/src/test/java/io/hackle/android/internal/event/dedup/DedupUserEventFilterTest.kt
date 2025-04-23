package io.hackle.android.internal.event.dedup

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.android.internal.event.UserEvents
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DedupUserEventFilterTest {
    @Test
    fun `when event is dedup target then return BLOCK`() {
        // given
        val eventDedupDeterminer = mockk<UserEventDedupDeterminer>()
        every { eventDedupDeterminer.isDedupTarget(any()) } returns true

        val sut = DedupUserEventFilter(eventDedupDeterminer)

        // when
        val actual = sut.check(UserEvents.track("test"))

        // then
        expectThat(actual).isEqualTo(UserEventFilter.Result.BLOCK)
    }

    @Test
    fun `when event is`() {
        // given
        val eventDedupDeterminer = mockk<UserEventDedupDeterminer>()
        every { eventDedupDeterminer.isDedupTarget(any()) } returns false

        val sut = DedupUserEventFilter(eventDedupDeterminer)

        // when
        val actual = sut.check(UserEvents.track("test"))

        // then
        expectThat(actual).isEqualTo(UserEventFilter.Result.PASS)
    }
}
