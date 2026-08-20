package io.hackle.android.internal.invocator.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.hackle.android.HackleApp
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.HackleWebViewConfig
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.metrics.Metrics
import java.lang.ref.WeakReference

internal open class HackleJavascriptInterface(
    private val app: HackleApp,
    private val webViewConfig: HackleWebViewConfig,
) {

    @Volatile
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

    /**
     * JS 쪽은 요청 후 약 13초가 지나면 타임아웃으로 Promise를 reject하고 응답을 포기하는데,
     * native 쪽에는 그에 대응하는 자체 데드라인이 없다 — 실질적인 상한은 completion을 완료시키는
     * HTTP 클라이언트(OkHttp)의 timeout 설정뿐이다. 그 timeout들을 늘리면 native가 성공해도
     * JS가 이미 reject한 뒤에 회신이 도착할 수 있으니 주의한다.
     *
     * 이 콜백 람다는 `this`를 강하게 캡처하며 completion을 실어 나르는 `CompletableFuture` 체인이
     * 완료될 때까지 그 캡처를 붙들고 있으므로, [webViewRef]의 `WeakReference`는 필드 자체만 보호할 뿐
     * 이 리텐션 경로까지 막아주지는 않는다.
     */
    private fun resolve(requestId: String, response: String) {
        val webView = webViewRef?.get()
        if (webView == null) {
            log.debug { "Skipped bridge resolve. [requestId=$requestId]" }
            return
        }
        val script = "window._hackleBridge && window._hackleBridge.resolve(${requestId.toJson()}, ${response.toJson()})"
        runOnUiThread {
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
