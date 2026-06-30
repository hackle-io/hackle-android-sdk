package io.hackle.android.internal.sync

import io.hackle.android.internal.task.Task
import io.hackle.sdk.core.internal.log.Logger

internal interface Synchronizer {
    fun sync(): Task<Unit>
}

private val log = Logger<Synchronizer>()

internal fun Synchronizer.safeSync(): Task<Unit> {
    return sync()
        .recover { log.error { "Failed to sync ${javaClass.simpleName}: $it" } }
}
