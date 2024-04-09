package io.hackle.android.internal.utils.concurrent

internal class Throttler(
    private val limiter: ThrottleLimiter
) {
    fun execute(accept: () -> Unit, reject: () -> Unit) {
        if (limiter.tryAcquire()) {
            accept()
        } else {
            reject()
        }
    }
}
