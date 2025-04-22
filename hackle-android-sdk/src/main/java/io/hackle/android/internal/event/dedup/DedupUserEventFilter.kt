package io.hackle.android.internal.event.dedup

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.android.internal.event.UserEventFilter.Result.BLOCK
import io.hackle.android.internal.event.UserEventFilter.Result.PASS
import io.hackle.sdk.core.event.UserEvent

internal class DedupUserEventFilter(
    private val eventDedupDeterminer: UserEventDedupDeterminer
) : UserEventFilter {
    override fun check(event: UserEvent): UserEventFilter.Result {
        val isDedupTarget = eventDedupDeterminer.isDedupTarget(event)
        return if (isDedupTarget) BLOCK else PASS
    }

    override fun filter(event: UserEvent): UserEvent  = event
}
