package io.hackle.android.internal.invocator.web

import android.webkit.JavascriptInterface
import io.hackle.android.HackleAppMode
import io.hackle.android.internal.model.Sdk
import io.hackle.sdk.common.HackleInvocator

internal class HackleJavascriptInterface(
    private val invocator: HackleInvocator,
    private val sdk: Sdk,
    private val mode: HackleAppMode
) {

    @JavascriptInterface
    fun getAppSdkKey(): String {
        return sdk.key
    }

    @JavascriptInterface
    fun getInvocationType(): String {
        return "function"
    }

    @JavascriptInterface
    fun getAppMode(): String {
        return mode.name
    }

    @JavascriptInterface
    fun invoke(string: String): String {
        return invocator.invoke(string)
    }

    companion object {
        const val NAME = "_hackleApp"
    }
}
