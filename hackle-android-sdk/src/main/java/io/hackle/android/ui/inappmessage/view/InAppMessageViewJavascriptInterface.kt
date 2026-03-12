package io.hackle.android.ui.inappmessage.view

import android.webkit.JavascriptInterface
import io.hackle.android.HackleApp
import io.hackle.android.internal.invocator.web.HackleJavascriptInterface
import io.hackle.sdk.common.HackleWebViewConfig

internal class InAppMessageViewJavascriptInterface(
    app: HackleApp,
    private val view: InAppMessageView,
) : HackleJavascriptInterface(app, WEB_VIEW_CONFIG) {

    @JavascriptInterface
    fun getInAppMessageViewId(): String {
        return view.id
    }

    companion object {
        private val WEB_VIEW_CONFIG = HackleWebViewConfig.builder()
            .automaticRouteTracking(false)
            .automaticScreenTracking(false)
            .automaticEngagementTracking(false)
            .build()
    }
}
