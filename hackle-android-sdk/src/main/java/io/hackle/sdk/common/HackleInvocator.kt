package io.hackle.sdk.common

interface HackleInvocator {
    fun isInvocableString(string: String): Boolean
    fun invoke(string: String): String
}
