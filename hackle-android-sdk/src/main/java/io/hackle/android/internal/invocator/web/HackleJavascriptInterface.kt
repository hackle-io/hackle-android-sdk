package io.hackle.android.internal.invocator.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.hackle.android.HackleApp
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.HackleWebViewConfig

internal open class HackleJavascriptInterface(
    private val app: HackleApp,
    private val webViewConfig: HackleWebViewConfig,
) {
    @JavascriptInterface
    fun getAppSdkKey(): String {
        return app.sdk.key
    }

    @JavascriptInterface
    fun getAppMode(): String {
        return app.config.mode.name
    }

    @JavascriptInterface
    fun getWebViewConfig(): String {
        return webViewConfig.toJson()
    }

    @JavascriptInterface
    fun getInvocationType(): String {
        return "function"
    }

    @JavascriptInterface
    fun invoke(string: String): String {
        return app.invocator.invoke(string)
    }

    fun addTo(webView: WebView) {
        webView.addJavascriptInterface(this, NAME)
    }

    companion object {
        const val NAME = "_hackleApp"
    }
}
