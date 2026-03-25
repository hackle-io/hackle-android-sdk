package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import io.hackle.android.ui.inappmessage.InAppMessageUi

internal class InAppMessageWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr), InAppMessageViewAware {

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && InAppMessageUi.instance.isBackButtonDismisses) {
            messageView?.close()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun load(html: String) {
        loadDataWithBaseURL(ASSET_LOADER_BASE_URL, html, TEXT_HTML_MIME_TYPE, UTF_8_ENCODING, null)
    }

    companion object {
        const val TEXT_HTML_MIME_TYPE = "text/html"
        const val UTF_8_ENCODING = "utf-8"

        private const val ASSET_LOADER_DOMAIN = "cache.hackle"
        const val ASSET_LOADER_BASE_URL = "https://$ASSET_LOADER_DOMAIN"

        fun createAssetLoader(context: Context): WebViewAssetLoader {
            return WebViewAssetLoader.Builder()
                .setDomain(ASSET_LOADER_DOMAIN)
                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
        }
    }
}
