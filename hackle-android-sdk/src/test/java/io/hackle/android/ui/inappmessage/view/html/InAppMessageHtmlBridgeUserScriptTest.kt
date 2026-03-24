package io.hackle.android.ui.inappmessage.view.html

import io.hackle.android.HackleConfig
import io.hackle.android.ui.inappmessage.view.InAppMessageWebView.Companion.ASSET_LOADER_BASE_URL
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.BRIDGE_FUNCTION_NAME
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.JAVASCRIPT_SDK_ASSET
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File

internal class InAppMessageHtmlBridgeUserScriptTest {

    @Test
    fun `JAVASCRIPT_SDK_ASSET - check fileName`() {
        expectThat(JAVASCRIPT_SDK_ASSET).isEqualTo("hackle-javascript-sdk-11.55.0.min.js")
    }

    @Test
    fun `JAVASCRIPT_SDK_ASSET - check file exists`() {
        val assetFile = File("src/main/assets", JAVASCRIPT_SDK_ASSET)
        expectThat(assetFile.exists()).isTrue()
    }

    @Test
    fun `BRIDGE_FUNCTION_NAME - check functionName`() {
        expectThat(BRIDGE_FUNCTION_NAME).isEqualTo("setAppWebViewInAppMessageBridge")
    }

    @Test
    fun `BRIDGE_FUNCTION_NAME - check js file`() {
        val jsFile = File("src/main/assets", JAVASCRIPT_SDK_ASSET)
        val jsContent = jsFile.readText()

        expectThat(jsContent).contains(BRIDGE_FUNCTION_NAME)
    }

    @Test
    fun `BRIDGE_FUNCTION_NAME - check kotlin source`() {
        val script = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        expectThat(script.source).contains(BRIDGE_FUNCTION_NAME)
    }


    @Test
    fun `create - default`() {
        val expectedDefaultUrl = "$ASSET_LOADER_BASE_URL/$JAVASCRIPT_SDK_ASSET"
        val sut = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        expectThat(sut.source).contains(expectedDefaultUrl)
    }

    @Test
    fun `create - custom`() {
        val expectedDefaultUrl = "$ASSET_LOADER_BASE_URL/$JAVASCRIPT_SDK_ASSET"
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

    @Test
    fun `source contains script injection elements`() {
        val sut = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        val source = sut.source

        expectThat(source) {
            contains("document.createElement('script')")
            contains("Hackle.$BRIDGE_FUNCTION_NAME()")
            contains("document.head.appendChild(s)")
        }
    }
}
