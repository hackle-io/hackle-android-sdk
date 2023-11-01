package io.hackle.android.internal.bridge.web

import android.webkit.JavascriptInterface
import io.hackle.android.HackleApp
import io.hackle.android.internal.bridge.HackleBridge

@Suppress("unused")
internal class HackleJavascriptInterface(app: HackleApp) {

    private val bridge = HackleBridge(app)

    @JavascriptInterface
    fun getAppSdkKey(): String {
        return bridge.getAppSdkKey()
    }

    @JavascriptInterface
    fun getInvocationType(): String {
        return "function"
    }

    @JavascriptInterface
    fun invoke(string: String): String {
        return bridge.invoke(string)
    }

    companion object {
        const val NAME = "_hackleApp"
    }
}