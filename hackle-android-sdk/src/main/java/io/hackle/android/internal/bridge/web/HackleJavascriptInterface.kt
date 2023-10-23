package io.hackle.android.internal.bridge.web

import android.webkit.JavascriptInterface
import io.hackle.android.Hackle
import io.hackle.android.HackleApp
import io.hackle.android.app
import io.hackle.android.internal.bridge.HackleBridge

@Suppress("unused")
internal class HackleJavascriptInterface(app: HackleApp) {

    companion object {
        const val NAME = "_hackleApp"
    }

    private val bridge = HackleBridge(app)

    @JavascriptInterface
    fun getAppSdkKey(): String {
        return bridge.getAppSdkKey()
    }

    @JavascriptInterface
    fun getInvocationType(): String {
        return "default"
    }

    @JavascriptInterface
    fun invoke(string: String): String? {
        return bridge.invoke(string)
    }
}