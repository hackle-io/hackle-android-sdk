package io.hackle.android.internal.mode.webview

import io.hackle.android.internal.event.UserEventFilter
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.user.hackleDeviceId
import io.hackle.sdk.core.event.UserEvent

internal class WebViewWrapperUserEventFilter : UserEventFilter {
    override fun check(event: UserEvent): UserEventFilter.Result {
        if (!PushEventTracker.isPushEvent(event)) {
            return UserEventFilter.Result.BLOCK
        }

        if (event.user.deviceId == event.user.hackleDeviceId) {
            return UserEventFilter.Result.BLOCK
        }

        return UserEventFilter.Result.PASS
    }
}
