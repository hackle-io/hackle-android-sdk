package io.hackle.android.ui.inappmessage.view

import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageWebViewClient(
    private val listener: PageListener,
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        listener.onPageFinished(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return listener.onUrlLoading(request.url.toString())
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return listener.onUrlLoading(url)
    }

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
