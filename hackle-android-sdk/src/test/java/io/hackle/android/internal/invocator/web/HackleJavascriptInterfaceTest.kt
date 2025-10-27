package io.hackle.android.internal.invocator.web

import io.hackle.android.HackleAppMode
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.HackleInvocatorImpl
import io.hackle.android.internal.model.Sdk
import io.hackle.sdk.common.HackleWebViewConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isContainedIn
import strikt.assertions.isEqualTo

class HackleJavascriptInterfaceTest {

    @Test
    fun name() {
        expectThat(HackleJavascriptInterface.NAME).isEqualTo("_hackleApp")
    }

    @Test
    fun getAppSdkKey() {
        val sut = HackleJavascriptInterface(invocation(), Sdk("SDK_KEY", "name", "version"), HackleAppMode.NATIVE, HackleWebViewConfig.DEFAULT)
        expectThat(sut.getAppSdkKey()).isEqualTo("SDK_KEY")
    }

    @Test
    fun getInvocationType() {
        val sut = HackleJavascriptInterface(invocation(), Sdk("SDK_KEY", "name", "version"), HackleAppMode.NATIVE, HackleWebViewConfig.DEFAULT)
        expectThat(sut.getInvocationType()).isEqualTo("function")
    }

    @Test
    fun getAppMode() {
        val sut =
            HackleJavascriptInterface(invocation(), Sdk("SDK_KEY", "name", "version"), HackleAppMode.WEB_VIEW_WRAPPER, HackleWebViewConfig.DEFAULT)
        expectThat(sut.getAppMode()).isEqualTo("WEB_VIEW_WRAPPER")
    }
    
    @Test
    fun getWebViewConfig() {
        val webViewConfig = HackleWebViewConfig.builder()
            .automaticScreenTracking(true)
            .build()
        val sut = HackleJavascriptInterface(invocation(), Sdk("SDK_KEY", "name", "version"), HackleAppMode.WEB_VIEW_WRAPPER, webViewConfig)
        expectThat(sut.getWebViewConfig()).isEqualTo("{\"automaticScreenTracking\":true}")
    }

    @Test
    fun invoke() {
        // given
        val invocation = mockk<HackleInvocatorImpl>()
        every { invocation.invoke(any()) } returns "result"
        val sut = HackleJavascriptInterface(invocation, Sdk("SDK_KEY", "name", "version"), HackleAppMode.NATIVE, HackleWebViewConfig.DEFAULT)

        // when
        val actual = sut.invoke("42")

        // then
        expectThat(actual).isEqualTo("result")
        verify(exactly = 1) {
            invocation.invoke("42")
        }
    }

    private fun invocation(): HackleInvocatorImpl {
        return HackleInvocatorImpl(mockk<HackleAppCore>(),)
    }
}
