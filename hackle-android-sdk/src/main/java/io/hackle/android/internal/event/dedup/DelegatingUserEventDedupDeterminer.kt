package io.hackle.android.internal.event.dedup

import io.hackle.sdk.core.event.UserEvent

internal class DelegatingUserEventDedupDeterminer(
    private val determiners: List<CachedUserEventDedupDeterminer<*, *>>
) : UserEventDedupDeterminer {
    override fun isDedupTarget(event: UserEvent): Boolean {
        val determiner = determiners.find { it.supports(event) } ?: return false
        return determiner.isDedupTarget(event)
    }
}
