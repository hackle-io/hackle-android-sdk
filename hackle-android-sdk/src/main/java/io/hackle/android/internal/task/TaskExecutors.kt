package io.hackle.android.internal.task

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import io.hackle.sdk.core.internal.threads.NamedThreadFactory
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal object TaskExecutors {

    private val MAIN_HANDLER = Handler(Looper.getMainLooper())
    private val BACKGROUND_WORKER = ThreadPoolExecutor(
        2, Int.MAX_VALUE,
        60, TimeUnit.SECONDS,
        SynchronousQueue(),
        NamedThreadFactory("HackleBackground-", true)
    )

    fun handler(name: String, priority: Int = THREAD_PRIORITY_BACKGROUND): Executor {
        val handlerThread = HandlerThread(name, priority)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        return HandlerExecutor(handler)
    }

    fun default(): ExecutorService {
        return BACKGROUND_WORKER
    }

    fun runOnUiThread(block: () -> Unit) {
        MAIN_HANDLER.post(block)
    }

    fun runOnBackground(block: () -> Unit) {
        BACKGROUND_WORKER.execute(block)
    }
}
