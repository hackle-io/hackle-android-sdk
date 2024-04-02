package io.hackle.android.internal.event.dedup

import io.hackle.sdk.core.event.UserEvent

internal interface UserEventDedupDeterminer {
    fun isDedupTarget(event: UserEvent): Boolean
}
