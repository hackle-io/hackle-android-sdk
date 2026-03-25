package io.hackle.android.ui.inappmessage.view

import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val listener: PageListener,
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        listener.onPageFinished(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(url.toUri())
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return listener.onUrlLoading(request.url.toString())
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return listener.onUrlLoading(url)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        log.debug { "WebView's render process has exited." }
        return true
    }

    interface PageListener {
        fun onPageFinished(view: WebView, url: String)
        fun onUrlLoading(url: String): Boolean
    }

    companion object {
        private val log = Logger<InAppMessageWebViewClient>()
    }
}

internal class InAppMessageWebChromeClient : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        log.debug { "InAppMessageWebView - [${consoleMessage.messageLevel()}] ${consoleMessage.message()}" }
        return true
    }

    companion object {
        private val log = Logger<InAppMessageWebChromeClient>()
    }
}
