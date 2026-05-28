package io.hackle.android.internal.platform.device

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DeviceHelperTest {

    @Test
    fun `getDisplayMetrics returns default metrics without crashing when DisplayManager returns null Display`() {
        // given
        val context = mockk<Context>()
        val displayManager = mockk<DisplayManager>()
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns displayManager
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns null

        // when - 현재 production 은 여기서 NPE
        val metrics = DeviceHelper.getDisplayMetrics(context)

        // then - default DisplayMetrics 반환 (모든 필드 0)
        expectThat(metrics.widthPixels).isEqualTo(0)
        expectThat(metrics.heightPixels).isEqualTo(0)
    }

    @Test
    fun `getDisplayMetrics returns default metrics without crashing when DISPLAY_SERVICE is not available`() {
        // given - IsolatedContext / instrumentation 환경 시뮬레이션
        val context = mockk<Context>()
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns null

        // when - 현재 production 은 (DisplayManager) cast 에서 ClassCastException
        val metrics = DeviceHelper.getDisplayMetrics(context)

        // then
        expectThat(metrics.widthPixels).isEqualTo(0)
        expectThat(metrics.heightPixels).isEqualTo(0)
    }

    @Test
    fun `getDisplayMetrics writes real metrics when Display is available`() {
        // given
        val context = mockk<Context>()
        val displayManager = mockk<DisplayManager>()
        val display = mockk<Display>()
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns displayManager
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns display
        every { display.getRealMetrics(any()) } answers {
            val m = firstArg<android.util.DisplayMetrics>()
            m.widthPixels = 1080
            m.heightPixels = 1920
        }

        // when
        val metrics = DeviceHelper.getDisplayMetrics(context)

        // then
        expectThat(metrics.widthPixels).isEqualTo(1080)
        expectThat(metrics.heightPixels).isEqualTo(1920)
    }
}
