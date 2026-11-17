package io.hackle.android.ui.inappmessage.view

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.JAVASCRIPT_SDK_ASSET
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import java.io.ByteArrayInputStream

internal class InAppMessageWebViewAssetLoaderTest {

    private val assets = mockk<AssetManager> {
        every { open(any(), any()) } answers { ByteArrayInputStream(byteArrayOf()) }
    }
    private val context = mockk<Context> {
        every { assets } returns this@InAppMessageWebViewAssetLoaderTest.assets
    }
    private val sut = InAppMessageWebView.createAssetLoader(context)

    @Test
    fun `serves the bundled javascript sdk`() {
        val response = sut.shouldInterceptRequest(uri("/$JAVASCRIPT_SDK_ASSET"))

        expectThat(response).isNotNull()
        verify(exactly = 1) { assets.open(JAVASCRIPT_SDK_ASSET, any()) }
    }

    @Test
    fun `does not open app assets other than the bundled javascript sdk`() {
        val paths = listOf(
            "/config.json",
            "/www/index.html",
            "/flutter_assets/AssetManifest.json",
            "/nested/$JAVASCRIPT_SDK_ASSET",
        )

        for (path in paths) {
            // handled locally so that the request never falls through to the network
            expectThat(sut.shouldInterceptRequest(uri(path))).describedAs(path).isNotNull()
        }
        verify(exactly = 0) { assets.open(any(), any()) }
    }

    private fun uri(path: String): Uri {
        return mockk {
            every { scheme } returns "https"
            every { authority } returns "cache.hackle"
            every { this@mockk.path } returns path
        }
    }
}
