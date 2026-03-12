package io.hackle.android.ui.core

import android.os.Build
import android.webkit.WebView

internal interface WebViewScript {
    fun build(): String
}

internal fun WebView.evaluate(script: WebViewScript) {
    val javascript = script.build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        evaluateJavascript(javascript, null)
    } else {
        loadUrl("javascript:$javascript")
    }
}
