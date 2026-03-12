package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

internal class InAppMessageWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr) {


    fun load(html: String) {
        loadDataWithBaseURL(BASE_URL, html, TEXT_HTML_MIME_TYPE, UTF_8_ENCODING, null)
    }

    companion object {
        const val TEXT_HTML_MIME_TYPE = "text/html"
        const val UTF_8_ENCODING = "utf-8"
        const val BASE_URL = "https://cache.hackle"
    }
}
