package io.hackle.android.internal.inappmessage.delay

internal interface InAppMessageDelayTask {
    val delay: InAppMessageDelay
    fun cancel()
}
