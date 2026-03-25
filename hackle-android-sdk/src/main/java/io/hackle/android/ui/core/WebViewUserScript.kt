package io.hackle.android.ui.core

import android.os.Build
import android.webkit.WebView

internal interface WebViewUserScript {
    val source: String
}

internal fun WebView.evaluate(script: WebViewUserScript) {
    val source = script.source
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        evaluateJavascript(source, null)
    } else {
        loadUrl("javascript:$source")
    }
}
