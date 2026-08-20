package io.hackle.android.internal.invocator.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.hackle.android.HackleApp
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.HackleWebViewConfig
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Metrics
import java.lang.ref.WeakReference

internal open class HackleJavascriptInterface(
    private val app: HackleApp,
    private val webViewConfig: HackleWebViewConfig,
) {

    private var webViewRef: WeakReference<WebView>? = null

    @JavascriptInterface
    fun getAppSdkKey(): String {
        return app.sdk.key
    }

    @JavascriptInterface
    fun getAppMode(): String {
        return app.config.appMode.name
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
    fun getBridgeCapabilities(): String {
        return CAPABILITIES
    }

    @JavascriptInterface
    fun invoke(string: String): String {
        return app.invocator.invoke(string)
    }

    /**
     * message 채널의 수신 지점. 응답을 반환하지 않는다.
     * requestId가 있는 요청은 처리 완료 후 `window._hackleBridge.resolve`로 회신한다.
     */
    @JavascriptInterface
    fun postMessage(message: String) {
        Metrics.counter("webview.bridge.message").increment()

        val requestId = InvocationRequest.requestId(message)
        if (requestId == null) {
            app.invocator.invoke(message)
            return
        }
        app.invocator.invokeAsync(message) { response -> resolve(requestId, response) }
    }

    private fun resolve(requestId: String, response: String) {
        val webView = webViewRef?.get()
        if (webView == null) {
            log.debug { "Skipped bridge resolve. [requestId=$requestId]" }
            return
        }
        val script = "window._hackleBridge && window._hackleBridge.resolve(${requestId.toJson()}, ${response.toJson()})"
        webView.post {
            try {
                webView.evaluateJavascript(script, null)
            } catch (e: Throwable) {
                log.debug { "Failed to resolve bridge request. [requestId=$requestId, error=$e]" }
            }
        }
    }

    fun addTo(webView: WebView) {
        webViewRef = WeakReference(webView)
        webView.addJavascriptInterface(this, NAME)
    }

    companion object {
        const val NAME = "_hackleApp"
        private const val CAPABILITIES = """["function","message"]"""
        private val log = Logger<HackleJavascriptInterface>()
    }
}
