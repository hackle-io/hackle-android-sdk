package io.hackle.android.ui.core

import android.webkit.ValueCallback
import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class WebViewUserScriptTest {

    @Test
    fun `evaluate passes completion to WebView evaluateJavascript`() {
        val webView = mockk<WebView>(relaxed = true)
        val callbackSlot = slot<ValueCallback<String>>()
        val script = object : WebViewUserScript {
            override val source: String = "1 + 1"
        }
        var completedWith: String? = null

        every { webView.evaluateJavascript("1 + 1", capture(callbackSlot)) } answers {
            callbackSlot.captured.onReceiveValue("2")
        }

        webView.evaluate(script) { value ->
            completedWith = value
        }

        verify(exactly = 1) { webView.evaluateJavascript("1 + 1", any()) }
        expectThat(completedWith).isEqualTo("2")
    }
}
