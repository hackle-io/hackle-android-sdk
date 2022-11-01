package io.hackle.android.internal.task

import android.os.Handler
import java.util.concurrent.Executor

internal class HandlerExecutor(private val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }
}
