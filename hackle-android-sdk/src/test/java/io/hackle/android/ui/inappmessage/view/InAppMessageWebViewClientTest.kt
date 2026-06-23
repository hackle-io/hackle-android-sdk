package io.hackle.android.ui.inappmessage.view

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

internal class InAppMessageWebViewClientTest {

    private val assetLoader = mockk<WebViewAssetLoader>(relaxed = true)
    private val listener = mockk<InAppMessageWebViewClient.PageListener>(relaxed = true)
    private val sut = InAppMessageWebViewClient(assetLoader, listener)

    @Test
    fun `onReceivedError on main frame notifies page error`() {
        val request = mockk<WebResourceRequest> { every { isForMainFrame } returns true }
        val error = mockk<WebResourceError>(relaxed = true)

        sut.onReceivedError(mockk(relaxed = true), request, error)

        verify(exactly = 1) { listener.onPageError() }
    }

    @Test
    fun `onReceivedError on sub resource is ignored`() {
        val request = mockk<WebResourceRequest> { every { isForMainFrame } returns false }
        val error = mockk<WebResourceError>(relaxed = true)

        sut.onReceivedError(mockk(relaxed = true), request, error)

        verify(exactly = 0) { listener.onPageError() }
    }

    @Test
    fun `deprecated onReceivedError notifies page error`() {
        @Suppress("DEPRECATION")
        sut.onReceivedError(mockk(relaxed = true), -1, "error", "https://example.com")

        verify(exactly = 1) { listener.onPageError() }
    }

    @Test
    fun `onRenderProcessGone notifies render process gone and returns true`() {
        val detail = mockk<RenderProcessGoneDetail>(relaxed = true)

        val result = sut.onRenderProcessGone(mockk(relaxed = true), detail)

        verify(exactly = 1) { listener.onRenderProcessGone() }
        verify(exactly = 0) { listener.onPageError() }
        expectThat(result).isTrue()
    }
}
