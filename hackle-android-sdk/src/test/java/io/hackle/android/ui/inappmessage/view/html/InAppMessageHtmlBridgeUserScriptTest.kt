package io.hackle.android.ui.inappmessage.view.html

import io.hackle.android.HackleConfig
import io.hackle.android.ui.inappmessage.view.InAppMessageWebView.Companion.ASSET_LOADER_BASE_URL
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File

internal class InAppMessageHtmlBridgeUserScriptTest {

    // Test-owned expected value — must be updated together with the JS SDK file.
    // If this value is stale, the tests below will pinpoint which step was missed.
    private val expectedAsset = "hackle-javascript-sdk-11.55.0.min.js"

    private val actualAsset: String = InAppMessageHtmlBridgeUserScript.JAVASCRIPT_SDK_ASSET

    // --- javascriptSdkResource consistency ---

    @Test
    fun `JAVASCRIPT_SDK_ASSET matches the test-expected file name`() {
        expectThat(actualAsset).isEqualTo(expectedAsset)
    }

    @Test
    fun `JS SDK asset file exists in assets directory`() {
        val assetFile = File("src/main/assets", actualAsset)
        expectThat(assetFile.exists()).isTrue()
    }

    // --- create(config:) ---

    @Test
    fun `create with default config uses default URL containing expected asset`() {
        val expectedDefaultUrl = "$ASSET_LOADER_BASE_URL/$expectedAsset"
        val sut = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        expectThat(sut.source).contains(expectedDefaultUrl)
    }

    @Test
    fun `create with override URL uses custom URL`() {
        val expectedDefaultUrl = "$ASSET_LOADER_BASE_URL/$expectedAsset"
        val customUrl = "https://cdn.example.com/custom-sdk.js"
        val config = HackleConfig.builder()
            .add("\$javascript_sdk_url", customUrl)
            .build()

        val sut = InAppMessageHtmlBridgeUserScript.create(config)

        expectThat(sut.source) {
            contains(customUrl)
            not { contains(expectedDefaultUrl) }
        }
    }

    // --- source ---

    @Test
    fun `source contains script injection elements`() {
        val sut = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        val source = sut.source

        expectThat(source) {
            contains("document.createElement('script')")
            contains("Hackle.setWebAppInAppMessageHtmlBridge()")
            contains("document.head.appendChild(s)")
        }
    }
}
