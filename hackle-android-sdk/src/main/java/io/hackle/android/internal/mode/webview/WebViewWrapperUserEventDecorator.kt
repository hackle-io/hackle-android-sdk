package io.hackle.android.internal.mode.webview

import io.hackle.android.internal.event.UserEventDecorator
import io.hackle.sdk.core.event.UserEvent

internal class WebViewWrapperUserEventDecorator : UserEventDecorator {
    override fun decorate(event: UserEvent): UserEvent {
        val user = event.user.toBuilder()
            .clearProperties()
            .build()

        return event.with(user)
    }
}
