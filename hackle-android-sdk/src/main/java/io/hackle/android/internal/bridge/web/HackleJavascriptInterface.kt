package io.hackle.android.internal.bridge.web

import android.webkit.JavascriptInterface
import io.hackle.android.internal.bridge.HackleBridge

internal class HackleJavascriptInterface(private val bridge: HackleBridge) {

    @JavascriptInterface
    fun getAppSdkKey(): String {
        return bridge.app.sdk.key
    }

    @JavascriptInterface
    fun getInvocationType(): String {
        return "function"
    }

    @JavascriptInterface
    fun getAppMode(): String {
        return bridge.app.mode.name
    }

    @JavascriptInterface
    fun invoke(string: String): String {
        return bridge.invoke(string)
    }

    companion object {
        const val NAME = "_hackleApp"
    }
}
