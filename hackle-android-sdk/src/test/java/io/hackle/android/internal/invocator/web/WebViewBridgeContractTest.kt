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
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.sdk.common.HackleWebViewConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

/**
 * 3 repo(iOS/Android/JS)가 공유하는 message 채널 wire 포맷 계약.
 * 여기의 payload 문자열은 스펙(§3)의 골든 픽스처다 — 임의로 바꾸지 말 것.
 */
internal class WebViewBridgeContractTest {

    private val requestId = "11111111-2222-3333-4444-555555555555"
    private val mutationInvoke =
        """{"_hackle":{"command":"setUser","parameters":{"user":{"id":"42"}},"requestId":"$requestId"}}"""
    private val trackInvoke =
        """{"_hackle":{"command":"track","parameters":{"event":{"key":"purchase"}}}}"""

    private lateinit var core: HackleAppCore
    private lateinit var app: HackleApp
    private lateinit var webView: WebView

    @Suppress("DEPRECATION")
    @Before
    fun setup() {
        mockkObject(TaskExecutors)
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }

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

    @After
    fun tearDown() {
        unmockkObject(TaskExecutors)
    }

    private fun bridge(): HackleJavascriptInterface {
        val sut = HackleJavascriptInterface(app = app, webViewConfig = HackleWebViewConfig.DEFAULT)
        sut.addTo(webView)
        return sut
    }

    @Test
    fun `message 채널 진입점 이름은 postMessage로 고정이다`() {
        expectThat(HackleJavascriptInterface.NAME).isEqualTo("_hackleApp")
        expectThat(bridge().getBridgeCapabilities()).isEqualTo("""["function","message"]""")
        expectThat(bridge().getInvocationType()).isEqualTo("function")
    }

    @Test
    fun `requestId가 붙어도 기존 invocator가 그대로 파싱한다`() {
        expectThat(InvocationRequest.isInvocableString(mutationInvoke)).isTrue()
        expectThat(InvocationRequest.isInvocableString(trackInvoke)).isTrue()

        val request = InvocationRequest.parse(mutationInvoke)

        expectThat(request) {
            get { command }.isEqualTo(InvocationCommand.SET_USER)
            get { requestId }.isEqualTo(this@WebViewBridgeContractTest.requestId)
            get { browserProperties }.isEqualTo(emptyMap<String, Any>())
        }
    }

    @Test
    fun `mutation 메시지는 core를 호출하고 완료 후 resolve를 발송한다`() {
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
            """window._hackleBridge && window._hackleBridge.resolve("$requestId", "{\"success\":true,\"message\":\"OK\"}")"""
        )
    }

    @Test
    fun `track 메시지는 core를 호출하고 회신하지 않는다`() {
        bridge().postMessage(trackInvoke)

        verify(exactly = 1) { core.track(any(), any()) }
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }
}
