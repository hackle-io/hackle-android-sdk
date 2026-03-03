package io.hackle.android.internal.optout

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.sdk.core.event.UserEvent

internal class OptOutUserEventFilter(
    private val optOutManager: OptOutManager,
) : UserEventFilter {

    override fun check(event: UserEvent): UserEventFilter.Result {
        return if (optOutManager.isOptOut) {
            UserEventFilter.Result.BLOCK
        } else {
            UserEventFilter.Result.PASS
        }
    }
}
