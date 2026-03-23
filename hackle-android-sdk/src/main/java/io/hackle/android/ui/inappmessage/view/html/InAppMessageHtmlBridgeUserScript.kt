package io.hackle.android.ui.inappmessage.view.html

import io.hackle.android.HackleConfig
import io.hackle.android.ui.core.WebViewUserScript
import io.hackle.android.ui.inappmessage.view.InAppMessageWebView.Companion.ASSET_LOADER_BASE_URL

internal class InAppMessageHtmlBridgeUserScript(private val url: String) : WebViewUserScript {

    override val source: String
        get() {
            return """
               (function() {
                var s = document.createElement('script');
                s.src = '$url';
                s.onload = function() {
                    Hackle.setWebAppInAppMessageHtmlBridge();
                };
                document.head.appendChild(s);
            })();
        """.trimIndent()
        }

    companion object {
        private const val JAVASCRIPT_SDK_URL_KEY = "\$javascript_sdk_url"
        private const val JAVASCRIPT_SDK_ASSET = "hackle-javascript-sdk.min.js"
        private const val DEFAULT_JAVASCRIPT_SDK_URL = "$ASSET_LOADER_BASE_URL/$JAVASCRIPT_SDK_ASSET"

        fun create(config: HackleConfig): InAppMessageHtmlBridgeUserScript {
            val url = config[JAVASCRIPT_SDK_URL_KEY] ?: DEFAULT_JAVASCRIPT_SDK_URL
            return InAppMessageHtmlBridgeUserScript(url)
        }
    }
}
