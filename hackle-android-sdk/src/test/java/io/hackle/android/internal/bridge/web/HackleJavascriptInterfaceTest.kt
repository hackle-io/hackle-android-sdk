package io.hackle.android.internal.bridge.web

import io.hackle.android.HackleAppMode
import io.hackle.android.internal.HackleAppInternal
import io.hackle.android.internal.bridge.HackleBridge
import io.hackle.android.internal.model.Sdk
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleJavascriptInterfaceTest {

    @Test
    fun `name`() {
        expectThat(HackleJavascriptInterface.NAME).isEqualTo("_hackleApp")
    }

    @Test
    fun `getAppSdkKey`() {
        val sut = HackleJavascriptInterface(hackleBridge("SDK_KEY", HackleAppMode.NATIVE))
        expectThat(sut.getAppSdkKey()).isEqualTo("SDK_KEY")
    }

    @Test
    fun `getInvocationType`() {
        val sut = HackleJavascriptInterface(hackleBridge("SDK_KEY", HackleAppMode.NATIVE))
        expectThat(sut.getInvocationType()).isEqualTo("function")
    }

    @Test
    fun `getAppMode`() {
        val sut = HackleJavascriptInterface(hackleBridge("SDK_KEY", HackleAppMode.WEB_VIEW_WRAPPER))
        expectThat(sut.getAppMode()).isEqualTo("WEB_VIEW_WRAPPER")
    }

    @Test
    fun `invoke`() {
        // given
        val bridge = mockk<HackleBridge>()
        every { bridge.invoke(any()) } returns "result"
        val sut = HackleJavascriptInterface(bridge)

        // when
        val actual = sut.invoke("42")

        // then
        expectThat(actual).isEqualTo("result")
        verify(exactly = 1) {
            bridge.invoke("42")
        }
    }
    
    private fun hackleBridge(sdkKey: String, mode: HackleAppMode): HackleBridge {
        return HackleBridge(
            mockk<HackleAppInternal>(),
            Sdk(sdkKey, "name", "version"),
            mode
        )
    }
}
