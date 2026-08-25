package io.hackle.android.internal.invocator.web

import android.webkit.WebView
import io.hackle.android.HackleApp
import io.hackle.android.HackleAppMode
import io.hackle.android.HackleConfig
import io.hackle.android.internal.model.Sdk
import io.hackle.android.support.InlineUiThreadRule
import io.hackle.sdk.common.HackleInvocationCallback
import io.hackle.sdk.common.HackleWebViewConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class HackleJavascriptInterfaceTest {

    @get:Rule
    val inlineUiThread = InlineUiThreadRule()

    @Suppress("DEPRECATION")
    private fun app(
        sdk: Sdk = Sdk("SDK_KEY", "name", "version"),
        mode: HackleAppMode,
    ): HackleApp {
        return HackleApp(
            hackleAppCore = mockk(relaxed = true),
            sdk = sdk,
            config = HackleConfig.builder().mode(mode).build(),
            invocator = mockk(relaxed = true)
        )
    }

    @Test
    fun name() {
        expectThat(HackleJavascriptInterface.NAME).isEqualTo("_hackleApp")
    }

    @Test
    fun getAppSdkKey() {
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.NATIVE),
            webViewConfig = HackleWebViewConfig.DEFAULT
        )
        expectThat(sut.getAppSdkKey()).isEqualTo("SDK_KEY")
    }

    @Test
    fun getInvocationType() {
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.NATIVE),
            webViewConfig = HackleWebViewConfig.DEFAULT
        )
        expectThat(sut.getInvocationType()).isEqualTo("function")
    }

    @Test
    fun getAppMode() {
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.WEB_VIEW_WRAPPER),
            webViewConfig = HackleWebViewConfig.DEFAULT
        )
        expectThat(sut.getAppMode()).isEqualTo("WEB_VIEW_WRAPPER")
    }

    @Test
    fun getWebViewConfig() {
        val webViewConfig = HackleWebViewConfig.builder()
            .automaticRouteTracking(false)
            .automaticScreenTracking(true)
            .automaticEngagementTracking(true)
            .build()
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.NATIVE),
            webViewConfig = webViewConfig
        )
        expectThat(sut.getWebViewConfig()).isEqualTo("{\"automaticRouteTracking\":false,\"automaticScreenTracking\":true,\"automaticEngagementTracking\":true}")
    }

    @Test
    fun getWebViewConfigDefault() {
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.NATIVE),
            webViewConfig = HackleWebViewConfig.DEFAULT
        )
        expectThat(sut.getWebViewConfig()).isEqualTo("{\"automaticRouteTracking\":true,\"automaticScreenTracking\":false,\"automaticEngagementTracking\":false}")
    }

    @Test
    fun invoke() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        every { app.invocator.invoke(any()) } returns "result"
        val sut = HackleJavascriptInterface(
            app = app,
            webViewConfig = HackleWebViewConfig.DEFAULT
        )

        // when
        val actual = sut.invoke("42")

        // then
        expectThat(actual).isEqualTo("result")
        verify(exactly = 1) {
            app.invocator.invoke("42")
        }
    }

    @Test
    fun getSupportedInvocationTypes() {
        val sut = HackleJavascriptInterface(
            app = app(mode = HackleAppMode.NATIVE),
            webViewConfig = HackleWebViewConfig.DEFAULT
        )
        expectThat(sut.getSupportedInvocationTypes()).isEqualTo("""["function","message"]""")
    }

    @Test
    fun `postMessage - invoke할 수 없는 메시지는 무시한다`() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        val webView = mockk<WebView>(relaxed = true)
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)

        // when
        sut.postMessage("not a json")
        sut.postMessage("""{"_hackle":{"parameters":{}}}""")

        // then
        verify(exactly = 0) { app.invocator.invoke(any()) }
        verify(exactly = 0) { app.invocator.invokeAsync(any(), any()) }
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun `postMessage - messageId가 없으면 처리하지 않는다`() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        val webView = mockk<WebView>(relaxed = true)
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)

        // when
        sut.postMessage("""{"_hackle":{"command":"track","parameters":{"event":{"key":"purchase"}}}}""")

        // then
        verify(exactly = 0) { app.invocator.invoke(any()) }
        verify(exactly = 0) { app.invocator.invokeAsync(any(), any()) }
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun `postMessage - messageId가 있으면 완료 후 resolveMessage 스크립트를 실행한다`() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        val invocationCallback = slot<HackleInvocationCallback>()
        every { app.invocator.invokeAsync(any(), capture(invocationCallback)) } returns Unit
        val webView = mockk<WebView>(relaxed = true)
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)

        // when
        sut.postMessage("""{"_hackle":{"command":"resetUser","messageId":"msg-1"}}""")
        invocationCallback.captured.onResponse("""{"success":true,"message":"OK"}""")

        // then
        verify(exactly = 1) {
            webView.evaluateJavascript(
                """window._hackleBridge && window._hackleBridge.resolveMessage("msg-1", "{\"success\":true,\"message\":\"OK\"}")""",
                null
            )
        }
    }

    @Test
    fun `postMessage - evaluateJavascript가 예외를 던져도 전파하지 않는다`() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        val invocationCallback = slot<HackleInvocationCallback>()
        every { app.invocator.invokeAsync(any(), capture(invocationCallback)) } returns Unit
        val webView = mockk<WebView>(relaxed = true)
        every { webView.evaluateJavascript(any(), any()) } throws RuntimeException("boom")
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)

        // when
        sut.postMessage("""{"_hackle":{"command":"resetUser","messageId":"msg-1"}}""")
        invocationCallback.captured.onResponse("""{"success":true,"message":"OK"}""")
    }

    @Test
    fun `postMessage - WebView가 없으면 회신을 건너뛴다`() {
        // given
        val app = app(mode = HackleAppMode.NATIVE)
        val invocationCallback = slot<HackleInvocationCallback>()
        every { app.invocator.invokeAsync(any(), capture(invocationCallback)) } returns Unit
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        // WebView를 만들되 addTo로 연결하지 않는다
        val webView = mockk<WebView>(relaxed = true)

        // when
        sut.postMessage("""{"_hackle":{"command":"resetUser","messageId":"msg-1"}}""")
        invocationCallback.captured.onResponse("""{"success":true,"message":"OK"}""")

        // then
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }
}
