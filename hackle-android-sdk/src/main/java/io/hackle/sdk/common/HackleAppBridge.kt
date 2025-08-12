package io.hackle.sdk.common

interface HackleAppBridge {
    fun invoke(string: String): String
}
