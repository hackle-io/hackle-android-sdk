package io.hackle.android.internal.sync

import io.hackle.android.internal.task.recover
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.CompletableFuture

internal interface Synchronizer {
    fun sync(): CompletableFuture<Void>
}

private val log = Logger<Synchronizer>()

internal fun Synchronizer.safeSync(): CompletableFuture<Void> {
    return sync()
        .recover { log.error { "Failed to sync ${javaClass.simpleName}: $it" } }
}
