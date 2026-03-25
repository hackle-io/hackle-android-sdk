package io.hackle.android.ui.inappmessage.view.html

import io.hackle.android.HackleConfig
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.ui.inappmessage.view.InAppMessageWebView.Companion.ASSET_LOADER_BASE_URL
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.BRIDGE_FUNCTION_NAME
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.JAVASCRIPT_SDK_ASSET
import io.hackle.android.ui.inappmessage.view.html.InAppMessageHtmlBridgeUserScript.Companion.JAVASCRIPT_SDK_VERSION
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File

internal class InAppMessageHtmlBridgeUserScriptTest {

    @Test
    fun `JAVASCRIPT_SDK_ASSET - check fileName`() {
        expectThat(JAVASCRIPT_SDK_ASSET).isEqualTo("hackle-javascript-sdk-$JAVASCRIPT_SDK_VERSION.min.js")
    }

    @Test
    fun `JAVASCRIPT_SDK_ASSET - check file exists`() {
        val assetFile = File("src/main/assets", JAVASCRIPT_SDK_ASSET)
        expectThat(assetFile.exists()).isTrue()
    }

    @Test
    fun `JAVASCRIPT_SDK_ASSET - check script`() {
        val jsFile = File("src/main/assets", JAVASCRIPT_SDK_ASSET)
        val script = jsFile.readText()

        // Check Javascript SDK Version
        expectThat(script).contains(JAVASCRIPT_SDK_VERSION)

        // Check bridge function
        expectThat(script).contains(BRIDGE_FUNCTION_NAME)

        // Check InvocationCommand
        InvocationCommand.values()
            .filter { it != InvocationCommand.SET_CURRENT_SCREEN } // Not supported yet
            .forEach {
                expectThat(script).contains(it.command)
            }
    }

    @Test
    fun `BRIDGE_FUNCTION_NAME - check functionName`() {
        expectThat(BRIDGE_FUNCTION_NAME).isEqualTo("setAppWebViewInAppMessageBridge")
    }


    @Test
    fun `check inject source`() {
        val script = InAppMessageHtmlBridgeUserScript.create(HackleConfig.DEFAULT)
        expectThat(script.source) {
            contains("document.createElement('script')")
            contains("Hackle.$BRIDGE_FUNCTION_NAME()")
            contains("document.head.appendChild(s)")
        }
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
}
