package io.hackle.android.internal.invocator.web

import io.hackle.android.HackleApp
import io.hackle.android.HackleAppMode
import io.hackle.android.HackleConfig
import io.hackle.android.internal.model.Sdk
import io.hackle.sdk.common.HackleWebViewConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleJavascriptInterfaceTest {

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
}
