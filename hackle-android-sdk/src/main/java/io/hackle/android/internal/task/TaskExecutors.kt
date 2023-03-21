package io.hackle.android.internal.task

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal object TaskExecutors {

    private val MAIN_HANDLER = Handler(Looper.getMainLooper())
    private val WORKER by lazy { Executors.newFixedThreadPool(2) }

    fun handler(name: String, priority: Int = THREAD_PRIORITY_BACKGROUND): Executor {
        val handlerThread = HandlerThread(name, priority)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        return HandlerExecutor(handler)
    }

    fun runOnUiThread(block: () -> Unit) {
        MAIN_HANDLER.post(block)
    }

    fun runOnBackground(block: () -> Unit) {
        WORKER.execute(block)
    }
}
