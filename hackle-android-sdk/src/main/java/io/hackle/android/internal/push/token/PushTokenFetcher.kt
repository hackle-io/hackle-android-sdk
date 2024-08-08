package io.hackle.android.internal.push.token

import android.content.Context
import io.hackle.sdk.core.internal.log.Logger

internal interface PushTokenFetcher {
    fun fetch(): PushToken?
}

internal object PushTokenFetchers {

    private val log = Logger<PushTokenFetchers>()

    fun create(context: Context): PushTokenFetcher {
        return try {
            FcmPushTokenFetcher.create(context)
        } catch (e: Throwable) {
            log.debug { "Failed to instantiate FcmPushTokenFetcher: $e" }
            Noop
        }
    }

    private object Noop : PushTokenFetcher {
        override fun fetch(): PushToken? {
            return null
        }
    }
}
