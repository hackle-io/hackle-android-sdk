package io.hackle.android.internal.task

import android.os.Handler
import android.os.HandlerThread
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import java.util.concurrent.Executor

internal object TaskExecutors {

    fun handler(name: String, priority: Int = THREAD_PRIORITY_BACKGROUND): Executor {
        val handlerThread = HandlerThread(name, priority)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        return HandlerExecutor(handler)
    }
}
