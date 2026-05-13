package io.hackle.android.ui.inappmessage.view

import android.webkit.JavascriptInterface
import io.hackle.android.HackleApp
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.android.internal.invocator.web.HackleJavascriptInterface
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.HackleWebViewConfig
import io.hackle.sdk.core.internal.log.Logger

internal class InAppMessageViewJavascriptInterface(
    app: HackleApp,
    private val view: InAppMessageView,
    private val triggerEvent: Event,
) : HackleJavascriptInterface(app, WEB_VIEW_CONFIG) {

    @JavascriptInterface
    fun getInAppMessageViewId(): String {
        return view.id
    }

    @JavascriptInterface
    fun getInAppMessageTriggerEvent(): String {
        val json = runCatching { triggerEvent.toDto().toJson() }.getOrNull()
        if (json == null) {
            log.error { "Failed to serialize trigger event for HTML IAM bridge" }
            return ""
        }
        return json
    }

    companion object {
        private val log = Logger<InAppMessageViewJavascriptInterface>()
        private val WEB_VIEW_CONFIG = HackleWebViewConfig.builder()
            .automaticRouteTracking(false)
            .automaticScreenTracking(false)
            .automaticEngagementTracking(false)
            .build()
    }
}
