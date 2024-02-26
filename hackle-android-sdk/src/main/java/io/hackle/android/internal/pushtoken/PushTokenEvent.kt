package io.hackle.android.internal.pushtoken

import io.hackle.sdk.common.Event

internal data class RegisterPushTokenEvent(
    val token: String
)

internal fun RegisterPushTokenEvent.toTrackEvent() =
    Event.Builder("\$push_token")
        .property("provider_type", "FCM")
        .property("token", token)
        .build()