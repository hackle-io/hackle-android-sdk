package io.hackle.android.internal.push.token

import io.hackle.android.internal.push.PushPlatformType
import io.hackle.android.internal.push.PushProviderType

internal data class PushToken(
    val platformType: PushPlatformType,
    val providerType: PushProviderType,
    val value: String
) {

    companion object {
        fun of(value: String): PushToken {
            return PushToken(PushPlatformType.ANDROID, PushProviderType.FCM, value)
        }
    }
}
