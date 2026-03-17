package io.hackle.android.ui.inappmessage.view.html

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView
import androidx.annotation.RequiresApi
import io.hackle.android.Hackle
import io.hackle.android.R
import io.hackle.android.app
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.core.Animations
import io.hackle.android.ui.core.evaluate
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEvent
import io.hackle.android.ui.inappmessage.event.InAppMessageViewEventHandleType
import io.hackle.android.ui.inappmessage.view.InAppMessageAnimator
import io.hackle.android.ui.inappmessage.view.InAppMessageBaseView
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewJavascriptInterface
import io.hackle.android.ui.inappmessage.view.InAppMessageWebView
import io.hackle.android.ui.inappmessage.view.InAppMessageWebViewClient
import io.hackle.android.ui.inappmessage.view.handle
import io.hackle.android.ui.inappmessage.view.message
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.model.InAppMessage

/**
 * In-app message view that renders HTML content via WebView.
 *
 * Requires API 17+ because [android.webkit.WebView.addJavascriptInterface] is only safe
 * from JELLY_BEAN_MR1 onwards. Callers must gate on the API level before creating this view.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
internal class InAppMessageHtmlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : InAppMessageBaseView(context, attrs, defStyleAttr), InAppMessageView.LifecycleListener {

    // View
    private val webView: InAppMessageWebView get() = findViewById(R.id.hackle_iam_html_webview)

    // Model
    private val html: InAppMessage.Message.Html get() = requireNotNull(message.html) { "Not found Html [${inAppMessage.id}]" }

    // Animation
    override val openAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeIn(100))
    override val closeAnimator: InAppMessageAnimator get() = InAppMessageAnimator.of(this, Animations.fadeOut(100))

    private var _contentResolverFactory: InAppMessageHtmlContentResolverFactory? = null
    private val contentResolverFactory get() = requireNotNull(_contentResolverFactory) { "InAppMessageHtmlContentResolverFactory is not set on InAppMessageHtmlView." }

    private var _bridgeScript: InAppMessageHtmlBridgeUserScript? = null
    private val bridgeScript get() = requireNotNull(_bridgeScript) { "InAppMessageHtmlBridgeScript is not set on InAppMessageHtmlView." }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onConfigure(listener: InAppMessageView.ReadyListener) {

        // WebViewClient
        val pageListener = HtmlPageListener(listener)
        val webViewClient = InAppMessageWebViewClient(pageListener)

        // WebView
        webView.webViewClient = webViewClient
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.displayZoomControls = false
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false

        val javascriptInterface = InAppMessageViewJavascriptInterface(Hackle.app, this)
        javascriptInterface.addTo(webView)

        // Load html
        runOnBackground {
            resolveAndLoad()
        }
    }

    private fun resolveAndLoad() {
        try {
            val contentResolver = contentResolverFactory.get(html.resourceType)
            val content = contentResolver.resolve(html) // blocking
            runOnUiThread {
                webView.load(content)
            }
        } catch (e: Exception) {
            log.error { "Failed to resolve Html content: $e [${inAppMessage.id}]" }
            runOnUiThread { close() }
        }
    }

    override fun afterInAppMessageClose() {
        log.debug { "InAppMessageHtmlView.afterInAppMessageClose()" }
        webView.loadUrl("about:blank")
        webView.removeAllViews()
    }

    inner class HtmlPageListener(
        private val readyListener: InAppMessageView.ReadyListener
    ) : InAppMessageWebViewClient.PageListener {

        /**
         * Finalizes html view configuration.
         * - Evaluates bridge script. (Javascript SDK initialize)
         * - Notifies that the InAppMessageView is ready to be shown.
         */
        override fun onPageFinished(view: WebView, url: String) {
            if (state == InAppMessageView.State.CLOSED) {
                return
            }
            webView.evaluate(bridgeScript)
            readyListener.onReady()
        }

        override fun onUrlLoading(url: String): Boolean {
            val action = InAppMessage.Action(InAppMessage.Behavior.CLICK, InAppMessage.ActionType.WEB_LINK, url)
            val event = InAppMessageViewEvent.action(this@InAppMessageHtmlView, action, null)
            handle(event, InAppMessageViewEventHandleType.ACTION)
            return true
        }
    }

    companion object {
        private val log = Logger<InAppMessageHtmlView>()

        @SuppressLint("InflateParams")
        fun create(
            activity: Activity,
            bridgeScript: InAppMessageHtmlBridgeUserScript,
            contentResolverFactory: InAppMessageHtmlContentResolverFactory,
        ): InAppMessageHtmlView {
            val view = activity.layoutInflater.inflate(R.layout.hackle_iam_html, null)
            return (view as InAppMessageHtmlView).also {
                it._contentResolverFactory = contentResolverFactory
                it._bridgeScript = bridgeScript
            }
        }
    }
}
