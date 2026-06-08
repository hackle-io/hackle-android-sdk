package io.hackle.android.ui.core

import android.webkit.WebView

internal interface WebViewUserScript {
    val source: String
}

internal fun WebView.evaluate(script: WebViewUserScript) {
    val source = script.source
    evaluateJavascript(source, null)
}
