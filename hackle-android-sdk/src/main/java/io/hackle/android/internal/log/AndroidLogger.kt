package io.hackle.android.internal.log

import android.util.Log
import io.hackle.sdk.core.internal.log.Logger

internal object AndroidLogger : Logger {

    private const val TAG = "HackleSdk"

    override fun debug(msg: () -> String) = loggingIfEnabled(Log.DEBUG) { Log.d(TAG, msg()) }
    override fun info(msg: () -> String) = loggingIfEnabled(Log.INFO) { Log.i(TAG, msg()) }
    override fun warn(msg: () -> String) = loggingIfEnabled(Log.WARN) { Log.w(TAG, msg()) }
    override fun error(msg: () -> String) = loggingIfEnabled(Log.ERROR) { Log.e(TAG, msg()) }
    override fun error(x: Throwable, msg: () -> String) = loggingIfEnabled(Log.ERROR) { Log.e(TAG, msg(), x) }

    private inline fun loggingIfEnabled(level: Int, logging: () -> Unit) {
        if (Factory.logLevel <= level) {
            logging()
        }
    }

    object Factory : Logger.Factory {
        var logLevel: Int = Log.INFO
        override fun getLogger(name: String): Logger = AndroidLogger
    }
}