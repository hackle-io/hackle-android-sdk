package io.hackle.sdk.common

interface HackleInAppMessageView {
    val inAppMessage: HackleInAppMessage
    fun close()
}