package io.hackle.android.internal.pushtoken

import android.content.Context
import io.hackle.android.internal.pushtoken.datasource.EmptyPushTokenDataSource
import io.hackle.android.internal.pushtoken.datasource.ProviderType
import io.hackle.android.internal.pushtoken.datasource.PushTokenDataSource
import io.hackle.android.internal.pushtoken.datasource.fcm.FcmPushTokenDataSource
import io.hackle.sdk.core.internal.log.Logger

internal object PushTokenRegistration {

    private val log = Logger<PushTokenRegistration>()

    fun create(context: Context, providerType: ProviderType): PushTokenDataSource {
        when (providerType) {
            ProviderType.FCM -> {
                if (isAvailableFirebaseMessagingLibrary()) {
                    return FcmPushTokenDataSource(context)
                } else {
                    log.debug { "Not available firebase messaging library." }
                }
            }
        }
        return EmptyPushTokenDataSource()
    }

    private fun isAvailableFirebaseMessagingLibrary(): Boolean {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            return true
        } catch (_: Throwable) { }
        return false
    }
}