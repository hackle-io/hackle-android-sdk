package io.hackle.android.internal.push.token

import android.content.Context
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.Executor

internal interface PushTokenFetcher {
    fun fetch(callback: (PushToken?) -> Unit)
}

internal object PushTokenFetchers {

    private val log = Logger<PushTokenFetchers>()

    fun create(context: Context, executor: Executor): PushTokenFetcher {
        return try {
            FcmPushTokenFetcher.create(context, executor)
        } catch (e: Throwable) {
            log.debug { "Failed to instantiate FcmPushTokenFetcher: $e" }
            Noop
        }
    }

    private object Noop : PushTokenFetcher {
        override fun fetch(callback: (PushToken?) -> Unit) {
            callback(null)
        }
    }
}
