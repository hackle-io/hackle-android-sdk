package io.hackle.sdk.common

interface HackleAppBridge {
    fun isInvocableString(string: String): Boolean
    fun invoke(string: String): String
}
