package io.hackle.android.internal.invocator.web

import android.webkit.WebView
import io.hackle.android.HackleApp
import io.hackle.android.HackleAppMode
import io.hackle.android.HackleConfig
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.HackleInvocatorImpl
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.internal.invocator.invocation.InvocationHandlerFactory
import io.hackle.android.internal.invocator.invocation.InvocationProcessor
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.model.Sdk
import io.hackle.android.support.InlineUiThreadRule
import io.hackle.sdk.common.HackleWebViewConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

/**
 * 3 repo(iOS/Android/JS)가 공유하는 message 채널 wire 포맷 계약.
 * 여기의 payload 문자열은 스펙(§3)의 골든 픽스처다 — 임의로 바꾸지 말 것.
 */
internal class WebViewBridgeContractTest {

    private val messageId = "11111111-2222-3333-4444-555555555555"
    private val mutationInvoke =
        """{"_hackle":{"command":"setUser","parameters":{"user":{"id":"42"}},"messageId":"$messageId"}}"""
    private val trackInvoke =
        """{"_hackle":{"command":"track","parameters":{"event":{"key":"purchase"}}}}"""
    private val trackInvokeWithMessageId =
        """{"_hackle":{"command":"track","parameters":{"event":{"key":"purchase"}},"messageId":"$messageId"}}"""

    private lateinit var core: HackleAppCore
    private lateinit var app: HackleApp
    private lateinit var webView: WebView

    @get:Rule
    val inlineUiThread = InlineUiThreadRule()

    @Suppress("DEPRECATION")
    @Before
    fun setup() {
        core = mockk(relaxUnitFun = true)
        val invocator = HackleInvocatorImpl(InvocationProcessor(InvocationHandlerFactory(core)))
        app = HackleApp(
            hackleAppCore = core,
            sdk = Sdk("SDK_KEY", "name", "version"),
            config = HackleConfig.builder().mode(HackleAppMode.NATIVE).build(),
            invocator = invocator
        )
        webView = mockk(relaxed = true)
    }

    private fun bridge(): HackleJavascriptInterface {
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)
        return sut
    }

    @Test
    fun `message 채널 진입점 이름은 postMessage로 고정이다`() {
        expectThat(HackleJavascriptInterface.NAME).isEqualTo("_hackleApp")
        expectThat(bridge().getSupportedInvocationTypes()).isEqualTo("""["function","message"]""")
        expectThat(bridge().getInvocationType()).isEqualTo("function")
    }

    @Test
    fun `messageId가 붙어도 기존 invocator가 그대로 파싱한다`() {
        expectThat(InvocationRequest.isInvocableString(mutationInvoke)).isTrue()
        expectThat(InvocationRequest.isInvocableString(trackInvoke)).isTrue()

        val request = InvocationRequest.parse(mutationInvoke)

        expectThat(request) {
            get { command }.isEqualTo(InvocationCommand.SET_USER)
            get { messageId }.isEqualTo(this@WebViewBridgeContractTest.messageId)
            get { browserProperties }.isEqualTo(emptyMap<String, Any>())
        }
    }

    @Test
    fun `mutation 메시지는 core를 호출하고 완료 후 resolveMessage를 발송한다`() {
        // given
        val callback = slot<Runnable>()
        every { core.setUser(any(), capture(callback)) } returns Unit

        // when
        bridge().postMessage(mutationInvoke)

        // then: core 호출과 user 파싱
        verify(exactly = 1) { core.setUser(any(), any()) }

        // 완료 전에는 회신하지 않는다
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }

        // 완료되면 회신한다
        callback.captured.run()

        val script = slot<String>()
        verify(exactly = 1) { webView.evaluateJavascript(capture(script), null) }
        expectThat(script.captured).isEqualTo(
            """window._hackleBridge && window._hackleBridge.resolveMessage("$messageId", "{\"success\":true,\"message\":\"OK\"}")"""
        )
    }

    @Test
    fun `messageId가 없는 메시지는 core를 호출하지 않고 무시한다`() {
        bridge().postMessage(trackInvoke)

        verify(exactly = 0) { core.track(any(), any()) }
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun `mutation이 아닌 메시지도 messageId가 있으면 즉시 회신한다`() {
        bridge().postMessage(trackInvokeWithMessageId)

        verify(exactly = 1) { core.track(any(), any()) }

        val script = slot<String>()
        verify(exactly = 1) { webView.evaluateJavascript(capture(script), null) }
        expectThat(script.captured).isEqualTo(
            """window._hackleBridge && window._hackleBridge.resolveMessage("$messageId", "{\"success\":true,\"message\":\"OK\"}")"""
        )
    }
}
