package io.hackle.android.sdk.tester

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.hackle.android.HackleApp

class WebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("NewApi", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webview)

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.loadWithOverviewMode = true

        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)
        webView.settings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)
        HackleApp.getInstance().setWebViewBridge(webView)
        webView.loadUrl("")
    }
}